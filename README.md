# S60Launcher
An attempt to recreate the Symbian experience on Android as a launcher. I'll be honest, I don't know much about Android programming, so some of it was assisted by AI. Eventually I'd like to do a major rewrite to make it AI-free.

Accidentally imported all files to the master branch instead of the main branch lmao

## Features on last stable build:
-    Customizable pinned apps on home screen
-    Digital and analog clock (which you can switch)
-    Basic app customization(Can rename apps, hide apps and apply custom icons from images. No icon pack support yet)
-    Customizable soft keys on home screen (Settings and Gallery by default)
-    Working network indicator and battery indicator
-    Working calendar indicator and notifications

## Extra features from source code:
-    Status bar has been rewritten.
-    App drawer list view
-    Scroll bar / arrows
-    App themeing (Default S60v3 and S60v1 inspired themes)
-    Pinned apps rearranging (VERY broken)

## To-Do 
-    Make the status bar change dinamically depending on which part of the app you are. (Somewhat implemented)
-    Proper icon pack support
-    Improve app performance

## Bugs
-    Wallpaper doesn't load on newer Android releases
-    Pinned app's quick edit mode is broken
-    Some keypresses can sometimes register twice
-    There's some scrolling issues when going too fast (this seems to be an Android thing, would need to figure out a workaround)
