package net.basdon.shaderthing2;

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static java.nio.file.StandardWatchEventKinds.*;
import static javax.swing.SpringLayout.*;

public class ShaderThing2 extends JFrame implements WindowListener, ActionListener,
	ComponentListener, Runnable, ChangeListener, ItemListener, KeyListener
{
	public static void main(String args[])
	{
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {}
		new ShaderThing2();
	}

	final OGL ogl;
	final Canvas canvas;
	final SpringLayout layout;
	final JPanel cp;
	final TextArea txtError;
	final ScrollPane scrollPane;
	final Button btnTime, btnRewind, btnExport;
	final Checkbox chkDebugMov, chkCompiledCam;

	static File file;

	public static boolean debugmove, compiledcampath;

	Path watchingDir;
	WatchService watchService;
	WatchKey watchKey;
	long lastUpdate;
	long timerStart;
	boolean timerPaused;

	Point mousedown = new Point();
	float initialH, initialV;

	static class Var {
		float min, max, val;
		Label lblNum, lblVal;
		TextField txtMin, txtMax;
		JSlider slider;
		JComboBox<String> cmbSync;
		int sync;
		boolean suppressSliderChange, suppressUpdate;
	}

	static Var[] vars;

	public ShaderThing2()
	{
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(this);
		this.setMinimumSize(new Dimension(450, 400));
		this.setPreferredSize(new Dimension(450, 800));

		this.canvas = new Canvas();
		this.txtError = new TextArea();
		this.txtError.setVisible(false);
		JPanel content = new JPanel(new GridBagLayout());
		vars = new Var[8];
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 1;
		c.gridx = 1;
		c.gridwidth = 5;
		{
			JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
			this.btnTime = new Button("Pause timer");
			this.btnTime.addActionListener(this);
			btns.add(this.btnTime);
			this.btnRewind = new Button("Rewind timer");
			this.btnRewind.addActionListener(this);
			btns.add(this.btnRewind);
			this.btnExport = new Button("Export");
			this.btnExport.addActionListener(this);
			btns.add(this.btnExport);
			content.add(btns, c);
		}
		c.gridy++;
		{
			Panel chks = new Panel(new FlowLayout(FlowLayout.LEFT));
			this.chkDebugMov = new Checkbox("debugmove");
			this.chkDebugMov.addItemListener(this);
			chks.add(this.chkDebugMov);
			this.chkCompiledCam = new Checkbox("use compiled camera path");
			this.chkCompiledCam.addItemListener(this);
			chks.add(this.chkCompiledCam);
			content.add(chks, c);
		}
		c.gridwidth = 1;
		c.gridy++;
		{
			Label l;
			Font f = new JLabel().getFont().deriveFont(Font.BOLD);
			c.gridx = 1;
			content.add(l = new Label("n"), c); l.setFont(f);
			c.gridx = 2;
			content.add(l = new Label("min"), c); l.setFont(f);
			c.gridx = 3;
			c.weightx = 1d;
			content.add(l = new Label("x"), c); l.setFont(f);
			c.gridx = 4;
			c.weightx = 0d;
			content.add(l = new Label("cur"), c); l.setFont(f);
			c.gridx = 5;
			content.add(l = new Label("max"), c); l.setFont(f);
			c.gridx = 6;
			content.add(l = new Label("sync x with"), c); l.setFont(f);
		}
		c.gridy++;
		String[] cmbItems = new String[vars.length + 1];
		cmbItems[0] = "";
		for (int i = 0; i < vars.length; i++) {
			cmbItems[i + 1] = String.valueOf(i);
		}
		for (int i = 0; i < vars.length; i++) {
			c.gridy++;
			Var v = vars[i] = new Var();
			v.sync = -1;
			v.lblNum = new Label(String.valueOf(i));
			v.lblVal = new Label(String.valueOf(0f));
			v.slider = new JSlider(SwingConstants.HORIZONTAL, 0, 1000, 500);
			if (i == 0) {
				v.slider.setValue(1000);
				v.min = 0f;
				v.max = 0f;
			} else {
				v.min = -100f;
				v.max = 100f;
			}
			v.slider.addChangeListener(this);
			v.txtMin = new TextField(String.valueOf(v.min));
			v.txtMin.addKeyListener(this);
			v.txtMax = new TextField(String.valueOf(v.max));
			v.txtMax.addKeyListener(this);
			v.cmbSync = new JComboBox<>(cmbItems);
			v.cmbSync.addItemListener(this);
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;
			content.add(v.lblNum, c);
			c.gridx = 2;
			content.add(v.txtMin, c);
			c.gridx = 3;
			content.add(v.slider, c);
			c.gridx = 4;
			content.add(v.lblVal, c);
			c.gridx = 5;
			content.add(v.txtMax, c);
			c.gridx = 6;
			content.add(v.cmbSync, c);
		}
		this.scrollPane = new ScrollPane();
		this.scrollPane.add(content);

		this.cp = new JPanel(this.layout = new SpringLayout());
		this.cp.addComponentListener(this);
		this.cp.add(this.txtError);
		this.cp.add(this.canvas);
		this.cp.add(this.scrollPane);

		this.layout.putConstraint(NORTH, this.canvas, 0, NORTH, cp);
		this.layout.putConstraint(EAST, this.canvas, 0, EAST, cp);
		this.layout.putConstraint(WEST, this.canvas, 0, WEST, cp);
		this.layout.putConstraint(SOUTH, this.canvas, 225, NORTH, this.canvas);

		this.layout.putConstraint(NORTH, this.txtError, 0, NORTH, cp);
		this.layout.putConstraint(EAST, this.txtError, 0, EAST, cp);
		this.layout.putConstraint(SOUTH, this.txtError, 0, SOUTH, cp);
		this.layout.putConstraint(WEST, this.txtError, 0, WEST, cp);

		this.layout.putConstraint(NORTH, this.scrollPane, 0, SOUTH, this.canvas);
		this.layout.putConstraint(EAST, this.scrollPane, 0, EAST, cp);
		this.layout.putConstraint(WEST, this.scrollPane, 0, WEST, cp);
		this.layout.putConstraint(SOUTH, this.scrollPane, 0, SOUTH, cp);

		this.setContentPane(this.cp);
		this.pack();
		//this.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
		this.setLocationByPlatform(true);
		this.setVisible(true);
		this.ogl = new OGL(canvas);

		this.timerStart = System.currentTimeMillis();
		try {
			this.watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			errDlg(this, e, "failed to create watch service, shader will not reload");
		}

		if (2>1) {
			FileDialog fd = new FileDialog(this);
			fd.setMode(FileDialog.LOAD);
			fd.setMultipleMode(false);
			fd.setVisible(true);
			for (File file : fd.getFiles()) {
				ShaderThing2.file = file;
			}
		}
		//ShaderThing2.file = new File("D:/programming/demos/my - b - bv2022/frag.glsl");
		this.reloadShader();
		SwingUtilities.invokeLater(this);
		new CameraEditor();
	}

	/*WindowListener*/
	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	/*WindowListener*/
	@Override
	public void windowClosing(WindowEvent e)
	{
		this.ogl.destroy = true;
		try {
			this.ogl.join();
		} catch (InterruptedException e1) {
		}
		this.dispose();
		System.exit(0);
	}

	/*WindowListener*/
	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	/*WindowListener*/
	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	/*WindowListener*/
	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	/*WindowListener*/
	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	/*WindowListener*/
	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	/*ComponentListener*/
	@Override
	public void componentResized(ComponentEvent e)
	{
		float height = this.cp.getWidth() * 9f / 16f;
		this.layout.putConstraint(SOUTH, this.canvas, (int) height, NORTH, this.canvas);
		this.cp.doLayout();
	}

	/*ComponentListener*/
	@Override
	public void componentMoved(ComponentEvent e)
	{
	}

	/*ComponentListener*/
	@Override
	public void componentShown(ComponentEvent e)
	{
	}

	/*ComponentListener*/
	@Override
	public void componentHidden(ComponentEvent e)
	{
	}

	/*Runnable*/
	@Override
	public void run()
	{
		SwingUtilities.invokeLater(this);
		if (this.watchService != null) {
			WatchKey wk = this.watchService.poll();
			if (wk != null) {
				for (WatchEvent<?> evt : wk.pollEvents()) {
					if (evt.kind() == ENTRY_MODIFY && file != null) {
						Path path = (Path) evt.context();
						Path fn = path.getName(path.getNameCount() - 1);
						if (file.getName().equals(fn.toString())) {
							this.reloadShader();
						}
					}
				}
				if (!wk.reset()) {
					this.watchKey = null;
				}
			}
		}
		if (this.ogl.error != null) {
			this.txtError.setText(this.ogl.error);
			this.txtError.setVisible(true);
			this.ogl.error = null;
		}
		if (this.lastUpdate != this.ogl.timeCompiled) {
			this.setTitle("" + this.ogl.timeCompiled);
			this.lastUpdate = this.ogl.timeCompiled;
		}
		this.ogl.width = this.canvas.getWidth();
		this.ogl.height = this.canvas.getHeight();
		if (!this.timerPaused) {
			float t = (System.currentTimeMillis() - this.timerStart) / 1000f;
			vars[0].max = t;
			vars[0].txtMax.setText(String.format("%.1f", t));
			varUpdateVal(vars[0]);
		}
	}

	/*ChangeListener*/
	@Override
	public void stateChanged(ChangeEvent e)
	{
		Object src = e.getSource();
		for (int i = 0; i < vars.length; i++) {
			if (src == vars[i].slider) {
				varUpdateVal(vars[i]);
				return;
			}
		}
	}

	/*ItemListener*/
	@Override
	public void itemStateChanged(ItemEvent e)
	{
		Object src = e.getSource();
		if (src == this.chkDebugMov) {
			ShaderThing2.debugmove = this.chkDebugMov.getState();
		} else if (src == this.chkCompiledCam) {
			ShaderThing2.compiledcampath = this.chkCompiledCam.getState();
		} else {
			for (int i = 0; i < vars.length; i++) {
				if (src == vars[i].cmbSync) {
					int idx = vars[i].cmbSync.getSelectedIndex() - 1;
					if (idx == i) {
						return;
					}
					vars[i].sync = idx;
					varUpdateVal(vars[i]);
					return;
				}
			}
		}
	}

	/*KeyListener*/
	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	/*KeyListener*/
	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	/*KeyListener*/
	@Override
	public void keyReleased(KeyEvent e)
	{
		Object src = e.getSource();
		for (int i = 0; i < vars.length; i++) {
			Var v = vars[i];
			if (src == v.txtMin || src == v.txtMax) {
				String t = src == v.txtMin ? v.txtMin.getText() : v.txtMax.getText();
				if (t.startsWith(".")) {
					t = "0" + t;
				}
				if (t.endsWith(".")) {
					t = t + "0";
				}
				try {
					float f = Float.parseFloat(t);
					if (src == v.txtMin) {
						v.min = f;
					} else {
						v.max = f;
					}
					varUpdateVal(v);
				} catch (Throwable x) {
				}
				return;
			}
		}
	}

	/*ActionListener*/
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		if (src == this.btnTime) {
			this.timerPaused = !this.timerPaused;
			if (this.timerPaused) {
				this.btnTime.setLabel("Resume timer");
			} else {
				this.timerStart = System.currentTimeMillis();
				this.timerStart -= (long) (vars[0].max * 1000f);
				this.btnTime.setLabel("Pause timer");
			}
		} else if (src == this.btnRewind) {
			this.timerStart = System.currentTimeMillis();
			vars[0].max = 0f;
			varUpdateVal(vars[0]);
			vars[0].txtMax.setText(vars[0].lblVal.getText());
		} else if (src == this.btnExport && file != null) {
			ArrayList<String> nl;
			try {
				List<String> lines = Files.readAllLines(file.toPath());
				nl = new ArrayList<>(lines.size() + 2);
				nl.add("static char *fragSource =\n");
				for (String line : lines) {
					line = line.trim();
					if (line.endsWith("//noexport")) {
						continue;
					}
					int c = line.indexOf("//");
					if (c >= 0) {
						line = line.substring(0, c);
					}
					if (line.length() > 0) {
						if (line.startsWith("#")) {
							line += "\\n";
						}
						nl.add("\"" + line + "\"\n");
					}
				}
				nl.add(";");
			} catch (IOException i) {
				errDlg(this, i, "failed to read source: " + file.getName());
				return;
			}
			File out = new File(file.getParentFile(), file.getName() + ".c");
			try (FileOutputStream fos = new FileOutputStream(out, false)) {
				for (String line : nl) {
					fos.write(line.getBytes(StandardCharsets.UTF_8));
				}
				this.okDlg("ok");
			} catch (IOException i) {
				errDlg(this, i, "failed to write output: " + file.getName());
			}
		}
	}

	void updateCanvasMouse(int x, int y)
	{
		int dx = x - this.mousedown.x;
		int dy = y - this.mousedown.y;
		this.ogl.h = this.initialH + dx / 10f;
		this.ogl.v = this.initialV + dy / 10f;
	}

	static void varUpdateVal(Var v)
	{
		if (v.suppressUpdate) {
			return;
		}
		float val = v.max - v.min;
		if (val > 0) {
			if (v.sync != -1) {
				for (int i = 0; i < vars.length; i++) {
					if (i == v.sync) {
						v.suppressSliderChange = true;
						v.slider.setValue(vars[i].slider.getValue());
						v.suppressSliderChange = false;
					}
				}
			}
			val *= v.slider.getValue() / 1000f;
		}
		val += v.min;
		String st = String.format("%.1f", val);
		v.lblVal.setText(st);
		v.val = val;
		for (int i = 0; i < vars.length; i++) {
			if (vars[i] == v) {
				int idx = i;
				v.suppressUpdate = true;
				for (i = 0; i < vars.length; i++) {
					Var w = vars[i];
					if (w != v && w.sync == idx) {
						varUpdateVal(w);
					}
				}
				v.suppressUpdate = false;
				break;
			}
		}
	}

	void reloadShader()
	{
		if (file == null) {
			return;
		}
		try {
			byte[] bytes = Files.readAllBytes(file.toPath());
			this.txtError.setVisible(false);
			this.ogl.shaderToCompile = new String(bytes, StandardCharsets.UTF_8);
			this.ensureFileWatcher();
		} catch (IOException e) {
			errDlg(this, e, "failed to reader shader source: " + file.getName());
		}
	}

	void ensureFileWatcher()
	{
		if (file != null && this.watchService != null) {
			Path dir = file.getParentFile().toPath();
			if (!dir.equals(this.watchingDir)) {
				try {
					if (this.watchKey != null) {
						this.watchKey.cancel();
						this.watchKey = null;
					}
					this.watchKey = dir.register(this.watchService, ENTRY_MODIFY);
					this.watchingDir = dir;
					return;
				} catch (IOException e) {
					errDlg(this, e, "shader source file filewatcher failed");
				}
			}
		}
	}

	static void errDlg(Component parent, Throwable err, String message)
	{
		err.printStackTrace();
		String msg = String.format("%s\n\n%s", message, err.toString());
		JOptionPane.showMessageDialog(parent, msg);
	}

	void okDlg(String message)
	{
		JOptionPane.showMessageDialog(this, message);
	}
}
