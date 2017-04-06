-dontwarn android.support.**
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

-keep class org.gnucash.android.** {*;}
-keep class com.dropbox.** {*;}
-keep class android.support.v7.widget.SearchView { *; }