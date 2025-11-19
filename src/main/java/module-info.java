@SuppressWarnings("Java9RedundantRequiresStatement")
module library {
	requires java.sql;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires kotlin.stdlib;
	requires mapdb;
	requires org.jetbrains.annotations;
    requires static lombok;
    requires spring.beans;
    requires spring.ai.openai;
    requires spring.ai.model;
    requires spring.ai.client.chat;
    requires spring.webflux;
    requires spring.web;
    requires java.net.http;
    requires spring.context;
    requires jakarta.annotation;
    requires spring.boot;
    requires reactor.core;
    requires spring.ai.azure.openai;
    requires com.azure.ai.openai;
    requires spring.boot.autoconfigure;
    requires reactor.netty.http;

    exports library;
	exports library.controllers;
	exports library.controllers.author;
	exports library.controllers.common;
	exports library.controllers.librarian;
	exports library.controllers.student_staff;
	exports library.controls;
	exports library.models;
	exports library.persistence;
	exports library.utils;

	opens library;
	opens library.controllers;
	opens library.controllers.author;
	opens library.controllers.common;
	opens library.controllers.librarian;
	opens library.controllers.student_staff;
	opens library.controls;
	opens library.models;
	opens library.persistence;
	opens library.utils;
    exports library.SpringApplicationPackage;
    opens library.SpringApplicationPackage;
}
