module com.udacity.catpoint.security{
    requires com.udacity.catpoint.image;
    requires miglayout;
    requires java.sql;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;
    opens com.udacity.catpoint.security.data to com.google.gson;
}