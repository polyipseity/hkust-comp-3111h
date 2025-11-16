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

    exports library;
	exports library.controllers;
	exports library.controllers.author;
	exports library.controllers.librarian;
	exports library.controllers.student_staff;
	exports library.controls;
	exports library.models;
	exports library.persistence;
	exports library.utils;

	opens library;
	opens library.controllers;
	opens library.controllers.author;
	opens library.controllers.librarian;
	opens library.controllers.student_staff;
	opens library.controls;
	opens library.models;
	opens library.persistence;
	opens library.utils;
}
