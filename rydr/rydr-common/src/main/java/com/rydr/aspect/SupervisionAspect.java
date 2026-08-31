package com.rydr.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rydr.dto.government.SupervisionData;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Aspect operations for insert, update, and delete
 *
 * <p>Disabled unless {@code government-upload.enabled=true}. This aspect requires an
 * {@code ActiveMQQueue} and a {@code JmsMessagingTemplate} bean, which most services do not
 * define, so creating it unconditionally makes every module depending on rydr-common fail
 * to start with an unresolvable placeholder / missing bean error.
 *
 * @author yueyi2019
 * @date 2018/8/22
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "government-upload", name = "enabled", havingValue = "true")
public class SupervisionAspect {

    @NonNull
    private ActiveMQQueue bufferQueue;

    @NonNull
    private JmsMessagingTemplate jmsTemplate;

    /** Defaulted as well, so the placeholder can never fail to resolve. */
    @Value("${government-upload.enabled:false}")
    private boolean isGovernmentUploadEnabled;

    /**
     * Pointcut for insert operations
     */
    @Pointcut("execution(* com.rydr.mapper..*.insert*(..))")
    private void insert() {
    }

    /**
     * Pointcut for update operations
     */
    @Pointcut("execution(* com.rydr.mapper..*.update*(..))")
    private void update() {
    }

    /**
     * Pointcut for delete operations
     */
    @Pointcut("execution(* com.rydr.mapper..*.delete*(..))")
    private void delete() {
    }

    /**
     * Operation after successful insert
     *
     * @param joinPoint joinPoint
     */
    @AfterReturning(pointcut = "insert()")
    public void doAfterInsert(JoinPoint joinPoint) {
        send(SupervisionData.OperationType.INSERT, joinPoint);
    }

    /**
     * Operation after successful update
     *
     * @param joinPoint joinPoint
     */
    @AfterReturning(pointcut = "update()")
    public void doAfterUpdate(JoinPoint joinPoint) {
        send(SupervisionData.OperationType.UPDATE, joinPoint);
    }

    /**
     * Operation before delete
     *
     * @param joinPoint joinPoint
     */
    @Before("delete()")
    public void doBeforeDelete(JoinPoint joinPoint) {
        send(SupervisionData.OperationType.DELETE, joinPoint);
    }

    /**
     * Report to supervision system
     *
     * @param operationType operation type
     * @param joinPoint     joinPoint
     */
    @SuppressWarnings("unchecked")
    private void send(SupervisionData.OperationType operationType, JoinPoint joinPoint) {
        if (!isGovernmentUploadEnabled) {
            return;
        }

        Optional<Object> param = Arrays.stream(joinPoint.getArgs()).findFirst();
        if (param.isPresent()) {
            try {
                Object entity = param.get();
                Integer id;
                String clsName;
                if (entity instanceof Integer) {
                    id = (Integer) entity;
                    String packageName = joinPoint.getSignature().getDeclaringTypeName();
                    clsName = StringUtils.substringBeforeLast(packageName, "Mapper").replace("mapper", "entity");
                } else {
                    id = (Integer) entity.getClass().getDeclaredMethod("getId").invoke(entity);
                    clsName = param.get().getClass().getName();
                }
                SupervisionData data = new SupervisionData().setClassName(clsName).setId(id).setOperationType(operationType);
                String json = new ObjectMapper().writeValueAsString(data);
                jmsTemplate.convertAndSend(bufferQueue, json);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("SupervisionAspect error occurred: ", e);
            }
        }
    }

}
