package net.basdon.shaderthing2;

import java.awt.Canvas;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;

public class OGL extends Thread
{
	static final String VERT_SOURCE = String.join("\n",
		"#version 430",
		"layout (location=0) in vec2 i;",
		"out vec2 p;",
		"out gl_PerVertex",
		"{",
		"vec4 gl_Position;",
		"};",
		"void main() {",
		"gl_Position=vec4(p=i,0.,1.);",
		"}"
	);

	public boolean destroy;
	public Canvas parent;
	public String error;
	public String shaderToCompile;
	public long timeCompiled;
	public int width, height;

	int sProgram, sVert, sFrag;
	float x, y, z = -40f, h = 20f, v = 8.3f, initialH, initialV;
	int initialX, initialY;
	boolean mousedown;
	int delay;

	public OGL(Canvas parent)
	{
		this.sProgram = -1;
		this.sVert = -1;
		this.sFrag = -1;

		this.parent = parent;
		this.start();
	}

	@Override
	public void run()
	{
		try {
			//Display.setDisplayMode(new DisplayMode(800, 450));
			Display.setFullscreen(false);
			Display.setParent(this.parent);
			Display.create();
			Keyboard.create();
			Mouse.create();
			Keyboard.enableRepeatEvents(true);
			//GL11.glEnable(GL11.GL_TEXTURE_2D);
			//GL11.glShadeModel(GL11.GL_SMOOTH);
			//GL11.glDisable(GL11.GL_DEPTH_TEST);
			//GL11.glDisable(GL11.GL_LIGHTING);
			//GL11.glClearDepth(1);
			//GL11.glEnable(GL11.GL_BLEND);
			//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			//GL11.glViewport(0,0, 800, 450);
			//GL11.glMatrixMode(GL11.GL_MODELVIEW);
			//GL11.glMatrixMode(GL11.GL_PROJECTION);
			//GL11.glLoadIdentity();
			//GL11.glOrtho(0, 800, 450, 0, 1, -1);
			//GL11.glMatrixMode(GL11.GL_MODELVIEW);
		} catch (LWJGLException e) {
			e.printStackTrace();
			this.error = "failed to create opengl stuff";
			return;
		}
		int tex = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
		//GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		//GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		//GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
		float fromx = 0f, fromy = 0f;
		while (Display.isCreated() && !destroy) {
			if (this.shaderToCompile != null) {
				this.compileShader();
				fromx = 0f; fromy = 0f;
			}
			//if (++delay>2) {
				delay=0;
			GL11.glViewport(0, 0, this.width, this.height);
			if (this.sFrag != -1) {
				if (ShaderThing2.debugmove) {
					// if debugmov we render continuously
					GL41.glProgramUniform4f(this.sFrag, 0, this.width, this.height, fromx, fromy);
					GL41.glProgramUniform4f(this.sFrag, 2, x, y, z, ShaderThing2.debugmove ? 2f : 0f);
					GL41.glProgramUniform4f(this.sFrag, 3, h, v, 0, ShaderThing2.compiledcampath ? 2f : 0f);
					GL11.glRecti(1, 1, -1, -1);
				} else {
					boolean done = fromy > 1.2f;
					GL41.glProgramUniform4f(this.sFrag, 0, this.width, this.height, fromx, fromy);
					GL41.glProgramUniform4f(this.sFrag, 1, done ? 2.0f : 0.0f, 0f, 0f, 0f);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
					GL11.glRecti(1, 1, -1, -1);
					if (!done) {
						GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 0, 0, this.width, this.height, 0);
						fromx += .1f;
						if (fromx > 1.2f) {
							fromx = 0f;
							fromy += .1f;
						}
					}
				}

				//GL41.glProgramUniform1i(this.sFrag, 4, 0);
				//GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
				/*
				float f0, f1, f2, f3;
				for (int i = 0; i < ShaderThing2.vars.length / 4; i++) {
					f0 = ShaderThing2.vars[i * 4].val;
					f1 = ShaderThing2.vars[i * 4 + 1].val;
					f2 = ShaderThing2.vars[i * 4 + 2].val;
					f3 = ShaderThing2.vars[i * 4 + 3].val;
					GL41.glProgramUniform4f(this.sFrag, i, f0, f1, f2, f3);
				}
				/*
				float[] cam = CameraEditor.values();
				GL41.glProgramUniform4f(this.sFrag, 0, ShaderThing2.vars[0].val,
					ShaderThing2.vars[1].val,
					cam[3], cam[4]);
				GL41.glProgramUniform4f(this.sFrag, 0, 0, this.width, this.height, 0);
				GL41.glProgramUniform4f(this.sFrag, 1, cam[0], cam[1], cam[2],
					ShaderThing2.vars[7].val);
				GL41.glProgramUniform4f(this.sFrag, 2, x, y, z, ShaderThing2.debugmove ? 2f : 0f);
				GL41.glProgramUniform4f(this.sFrag, 3, h, v, 0, ShaderThing2.compiledcampath ? 2f : 0f);
				*/
			}
			//GL11.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
			//GL11.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
			//GL11.glRecti(1, 1, -1, -1);
			Display.update(false);
			//}
			Display.processMessages();
			while (Keyboard.next()) {
				char c = Keyboard.getEventCharacter();
				float H = h / 20f;
				if (!ShaderThing2.debugmove) {
					;
				} else if (c == 'z' || c == 'w') {
					x += Math.cos(H);
					y += Math.sin(H);
				} else if (c == 's') {
					x -= Math.cos(H);
					y -= Math.sin(H);
				} else if (c == 'q' || c == 'a') {
					x += Math.cos(H - Math.PI / 2f);
					y += Math.sin(H - Math.PI / 2f);
				} else if (c == 'd') {
					x -= Math.cos(H - Math.PI / 2f);
					y -= Math.sin(H - Math.PI / 2f);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_LSHIFT) {
					z -= 1;
				} else if (Keyboard.getEventKey() == Keyboard.KEY_LCONTROL) {
					z += 1;
				}
			}
			while (Mouse.next()) {
				if (Mouse.getEventButton() > -1 && ShaderThing2.debugmove) {
					boolean down = Mouse.getEventButtonState();
					if (down && !this.mousedown) {
						this.initialH = h;
						this.initialV = v;
						this.initialX = Mouse.getX();
						this.initialY = Mouse.getY();
					}
					this.mousedown = down;
				}
			}
			if (this.mousedown) {
				int dx = Mouse.getX() - this.initialX;
				int dy = Mouse.getY() - this.initialY;
				h = this.initialH + dx / 10f;
				v = this.initialV + dy / 10f;
			}
			/*
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
			Display.sync(60);
		}
		GL11.glDeleteTextures(tex);
		Display.destroy();
		Keyboard.destroy();
	}

	void compileShader()
	{
		String source = this.shaderToCompile;
		this.shaderToCompile = null;
		if (this.sVert == -1) {
			this.sVert = GL41.glCreateShaderProgram(GL20.GL_VERTEX_SHADER, VERT_SOURCE);
			if (GL20.glGetProgrami(this.sVert, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
				this.error = "vert fail\n" + GL20.glGetProgramInfoLog(this.sVert, 10000);
				this.sVert = -1;
				return;
			}
		}
		int sFrag = GL41.glCreateShaderProgram(GL20.GL_FRAGMENT_SHADER, source);
		if (GL20.glGetProgrami(sFrag, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
			this.error = "frag fail\n" + GL20.glGetProgramInfoLog(sFrag, 10000);
			return;
		}
		this.sFrag = sFrag;
		if (this.sProgram == -1) {
			this.sProgram = GL41.glGenProgramPipelines();
			GL41.glBindProgramPipeline(this.sProgram);
		}
		GL41.glUseProgramStages(this.sProgram, GL41.GL_VERTEX_SHADER_BIT, this.sVert);
		GL41.glUseProgramStages(this.sProgram, GL41.GL_FRAGMENT_SHADER_BIT, this.sFrag);
		if (GL41.glGetProgramPipelinei(this.sProgram, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
			this.error = "prog fail\n" + GL41.glGetProgramPipelineInfoLog(this.sProgram, 10000);
			this.sVert = -1;
			return;
		}
		this.timeCompiled = System.currentTimeMillis();
	}
}
