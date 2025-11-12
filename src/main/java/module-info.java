module library {
	requires javafx.controls;
	requires javafx.fxml;
	requires mapdb;
	requires annotations;
	requires static lombok;
	requires org.eclipse.collections.api;

	exports library;
	opens library.controllers;
	exports library.controllers;
	exports library.models;
	exports library.persistence;
	exports library.utils;
}
