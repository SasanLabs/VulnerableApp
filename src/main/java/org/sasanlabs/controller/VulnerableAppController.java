package org.sasanlabs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
public class VulnerableAppController {

	@RequestMapping("/")
	public String getTemplate() throws JsonProcessingException {
		return "index.html";
	}

}
