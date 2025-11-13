module library {
	requires javafx.controls;
	requires javafx.fxml;
	requires mapdb;
	requires org.jetbrains.annotations;
	requires static lombok;
	requires org.eclipse.collections.api;
	requires javafx.graphics;

	exports library;
	opens library.controllers;
	exports library.controllers;
	exports library.controls;
	exports library.models;
	exports library.persistence;
	exports library.utils;
}
