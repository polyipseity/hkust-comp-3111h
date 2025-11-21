module library {
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.github.librepdf.openpdf;
	requires java.desktop;
	requires java.net.http;
	requires java.prefs;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.swing;
	requires kotlin.stdlib;
	requires mapdb;
	requires org.bouncycastle.provider;
	requires org.icepdf.core;
	requires org.icepdf.ri.viewer;
	requires org.jetbrains.annotations;
	requires spring.boot;
	requires spring.web;
	requires static lombok;

	exports library;
	exports library.controllers;
	exports library.controllers.author;
	exports library.controllers.common;
	exports library.controllers.librarian;
	exports library.controllers.student_staff;
	exports library.controls;
	exports library.models;
	exports library.models.json;
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
	opens library.models.json;
	opens library.persistence;
	opens library.utils;
	exports library.SpringApplicationPackage;
	opens library.SpringApplicationPackage;
}
