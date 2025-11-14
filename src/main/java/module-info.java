module library {
	requires javafx.controls;
	requires javafx.fxml;
	requires mapdb;
	requires org.jetbrains.annotations;
	requires static lombok;
	requires org.eclipse.collections.api;
	requires javafx.graphics;
	requires kotlin.stdlib;
    requires java.desktop;

    exports library;
	exports library.controllers;
	exports library.controls;
	exports library.models;
	exports library.persistence;
	exports library.utils;
	exports library.controllers.student_staff;

	opens library.controllers;
	opens library.controllers.student_staff;
    exports library.controllers.author;
    opens library.controllers.author;
}
