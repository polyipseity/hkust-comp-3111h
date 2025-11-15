@SuppressWarnings("Java9RedundantRequiresStatement")
module library {
	requires javafx.controls;
	requires javafx.fxml;
	requires mapdb;
	requires org.jetbrains.annotations;
	requires static lombok;
	requires org.eclipse.collections.api;
	requires javafx.graphics;
	requires kotlin.stdlib;
    requires java.sql;
    requires library;
    requires javafx.base;

    exports library;
	exports library.controllers;
	exports library.controls;
	exports library.models;
	exports library.persistence;
	exports library.utils;
	exports library.controllers.student_staff;

	opens library;
	opens library.controllers;
	opens library.controls;
	opens library.models;
	opens library.persistence;
	opens library.utils;
	opens library.controllers.student_staff;
}
