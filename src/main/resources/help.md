# RoboKey
A Robot keyboard !
It emulates a keyboard and allows text to be typed on it.

it allows input from
1. The GUI user interface
2. The commandline when the app was run, just type a command
3. A Web interface (on port 8080 by default)
4. OS Named pipes, TO be implemented

It allows the text to be typed on either a 
1. LOCAL keyboard (An Java AWT Robot keyboard)
2. PHYSICAL keyboard (An Arduino Due, which would normally be connected to a different computer.)
3. LOG (Just logs the commands)


## PHYSICAL keyboard
 This code is mostly implemented on the Arduino Due it is just passed the commands by this software, you could actually talk to the Arduino directly over a
 The PHYSICAL keyboard is the most fully implemented, it allows settings such as jitter to be set to simulate an actual user interacting

## LOCAL keyboard
The LOCAL keyboard is simpler as it is mostly intended for testing. 
It has fixed delays and is not as configurable as the PHYSICAL keyboard

If you want to see the physical keystrokes then use the LOCAL keyboard, and set keyboard.output to false in the config file.

## LOG keyboard
This just logs the commands. eg. for LOREM it just logs the text LOREM not the full LOREM IPSUM text. 
If you want to see the physical keystrokes then use the LOCAL keyboard, with keyboard.output set to false in the config file.