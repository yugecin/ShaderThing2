A plaything to edit a fragment shader - written in a hurry for a demoscene
production, see https://github.com/yugecin/metro

Some screenshots are in this blog post:
https://robin.basdon.net/blog-003-metro-an-8KB-executable-demo.html

Running
-------

Lwjgl 2.9.3 is required to run:
https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl/2.9.3/lwjgl-2.9.3.jar

And natives for the correct platform:
https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl-platform/2.9.3/lwjgl-platform-2.9.3-natives-linux.jar
https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl-platform/2.9.3/lwjgl-platform-2.9.3-natives-windows.jar

extract the natives to some folder,
and add a JVM argument "-Djava.library.path=PATH/TO/THAT/FOLDER"

Manual [citation needed]
------------------------

On launch, it will open a file dialog to select a shader source file.

The shader source file can have a few uniforms:
> layout (location=0) uniform vec4 data[4];
which will be filled as follows:
data[0].x is the value of the slider 0 (which is the time)
data[0].y is the value of the slider 1
data[0].z is the value of the slider 2
data[0].w is the value of the slider 3
data[1].x is the value of the slider 4
data[1].y is the value of the slider 5
data[1].z is the value of the slider 6
data[1].w is the value of the slider 7
data[2].x is the debugcam position x
data[2].y is the debugcam position y
data[2].z is the debugcam position z
data[2].w is 0 or 2, depending on the "debugcam" checkbox state
data[3].x is the debugcam horizontal angle
data[3].y is the debugcam vertical angle
data[3].z is 0
data[3].w is 0 or 2, depending on the "use compiled camera path" checkbox state

when the "debugcam" checkbox is checked, wasd/zqsd & mousedrag &
shift(move up) & ctrl(move down) can be used to tinker with the "debugcam"
values

the "use compiled camera path" checkbox does nothing in particular except for
settings the uniform value

See the frag.glsl file in https://github.com/yugecin/metro to see how these
values can be used

Camera path editor
------------------

This is the window without a title that shows a bunch of curves.

Red points are start/end points, cyan points are pseudo control points.
Hold Shift while dragging a red point to keep the next red point at the same
value.
Right click between red points to create a new point at that position.
Right click before or after any red points to create a new one at the start/end.
These values are loaded from the file with the same name as the shader file
that is loaded plus ".pos". The load button discards any modifications and
reloads the values from the file. Saving will save the current values to this
file and create another file (same name plus ".c") with these values in a
syntax that can be used in a shader, in following order: "starttime,endtime,ystart,ycontrolpoint1,ycontrolpoint2,yend".

The sliders at the bottom either change the view or change the time
