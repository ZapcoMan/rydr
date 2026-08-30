package com.rydr.demo.websocket.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
/**
 *
 * @author oi
 * @date 2019-01-27 08:49:10
 */
@RestController
public class WelcomeController {
	/**
	 * Home page
	 *
	 * @return test
	 */
	@RequestMapping(value = "/welcome")
	@ResponseBody
	public String welcome() {
		return "Hello World";
	}

	/**
	 * WebSocket demo page.
	 * Spring Boot 3 does not support JSP with the embedded container,
	 * so the request is redirected to the static page /socket.html
	 */
	@RequestMapping(value = "/index")
	public String index() {
		return "redirect:/socket.html";
	}
}
