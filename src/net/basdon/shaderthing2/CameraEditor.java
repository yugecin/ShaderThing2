package net.basdon.shaderthing2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static java.lang.Math.*;
import static java.nio.file.StandardOpenOption.*;

public class CameraEditor extends JFrame implements ChangeListener, ActionListener
{
	static final int NUB = 4, N = NUB * 2 + 1, M = NUB + 1;

	JSlider scroll, time;

	static JSlider zoom;
	public static float getZoom() {
		return zoom.getValue();
	}

	public static Editor[] editors = new Editor[5];
	public static float[] values() {
		float t = ShaderThing2.vars[0].val;
		return new float[] {
			editors[0].val(t),
			editors[1].val(t),
			editors[2].val(t),
			editors[3].val(t),
			editors[4].val(t)
		};
	}

	CP dragging;
	int dragp;
	int lastx, lasty;
	public CameraEditor()
	{
		JPanel mid = new JPanel(new GridLayout(2, 3));
		mid.add(editors[0] = new Editor(-200, 100, 0, "x"));
		mid.add(editors[1] = new Editor(-800, 1600, 1, "y"));
		mid.add(editors[2] = new Editor(0, -80, 2, "z"));
		mid.add(editors[3] = new Editor(-6, 6, 3, "h"));
		mid.add(editors[4] = new Editor(-3, 0, 4, "v"));
		time = new JSlider(JSlider.HORIZONTAL, 0, 8000, 0);
		time.addChangeListener(this);
		scroll = new JSlider(JSlider.HORIZONTAL, 0, 10000, 0);
		scroll.addChangeListener(this);
		zoom = new JSlider(JSlider.HORIZONTAL, 1, 1000, 10);
		zoom.addChangeListener(this);
		JButton save = new JButton("save");
		save.addActionListener(this);
		JButton load = new JButton("load");
		load.addActionListener(this);
		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(save);
		buttons.add(load);
		JPanel bot = new JPanel(new BorderLayout());
		bot.add(zoom, BorderLayout.NORTH);
		bot.add(scroll, BorderLayout.CENTER);
		bot.add(buttons, BorderLayout.EAST);
		bot.add(time, BorderLayout.SOUTH);
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(bot, BorderLayout.SOUTH);
		this.getContentPane().add(mid, BorderLayout.CENTER);
		this.setLocationByPlatform(true);
		this.pack();
		//this.setVisible(true);
		//action("load");
	}

	public static class CP
	{
		float timea, timeb; // seconds
		float ay,
			py, //[0-1] in function of min/max value
			qy, //[0-1] in function of min/max value
			by;
		public CP(float timea, float timeb) {
			this.timea = timea;
			this.timeb = timeb;
			ay = .5f;
			py = .5f;
			qy = .5f;
			by = .5f;
		}
		CP prev, next;
	}

	public class Editor extends JPanel implements MouseListener, MouseMotionListener, Runnable
	{
		public CP pts;
		public int min, max, id;
		public Editor(int min, int max, int id, String name)
		{
			this.pts = new CP(0f, 5f);
			this.min = min;
			this.max = max;
			this.id = id;
			this.setName(name);
			this.setMinimumSize(new Dimension(400, 200));
			this.setPreferredSize(new Dimension(400, 200));
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			SwingUtilities.invokeLater(this);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			int w = this.getWidth(), h = this.getHeight();
			g.setColor(Color.black);
			g.fillRect(0, 0, w, h);
			g.setColor(Color.white);
			g.drawRect(0, 0, w, h);
			g.translate(-scroll.getValue(), 0);
			g.setColor(Color.yellow);
			float xx = ShaderThing2.vars[0].val * getZoom();
			g.drawLine((int) xx, 0, (int) xx, h);
			Nub n = new Nub();
			for (CP p = this.pts; p != null; p = p.next) {
				n.from(p, h);
				g.setColor(Color.white);
				for (float a = 0; a < 1; a += .01) {
					float b = 1f - a;
					float val = b*b*b*p.ay+3f*b*b*a*p.py+3f*b*a*a*p.qy+a*a*a*p.by;
					float x = n.ax + (n.bx - n.ax) * a;
					float y = val * h;
					g.fillRect((int) x - 1, (int) y - 1, 2, 2);
				}
				g.setColor(Color.red);
				g.fillRect((int) n.ax - NUB, (int) n.ay - NUB, N, N);
				g.fillRect((int) n.bx - NUB, (int) n.by - NUB, N, N);
				g.setColor(Color.cyan);
				g.fillRect((int) n.px - NUB, (int) n.py - NUB, N, N);
				g.fillRect((int) n.qx - NUB, (int) n.qy - NUB, N, N);
			}
			g.translate(scroll.getValue(), 0);
			g.setColor(Color.lightGray);
			g.fillRect(50, 0, 200, 14);
			g.setColor(Color.black);
			g.drawString(String.format("%s t %.2f v %.2f",
				this.getName(),
				(lastx + scroll.getValue()) / getZoom(),
				(this.max - this.min) * (float) lasty / h + this.min
				), 55, 12);
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			lastx = e.getX();
			lasty = e.getY();
			dodrag(e.getX(), e.getY(), e.isShiftDown());
		}

		@Override
		public void mouseMoved(MouseEvent e)
		{
			lastx = e.getX();
			lasty = e.getY();
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			int x = e.getX() + scroll.getValue(), y = e.getY();
			int h = this.getHeight();
			Nub n = new Nub();
			for (CP p = pts; p != null; p = p.next) {
				n.from(p, h);
				if (abs(x - n.ax) < M && abs(y - n.ay) < M) {
					dragging = p;
					dragp = 0;
					return;
				}
				if (abs(x - n.px) < M && abs(y - n.py) < M) {
					dragging = p;
					dragp = 1;
					return;
				}
				if (abs(x - n.qx) < M && abs(y - n.qy) < M) {
					dragging = p;
					dragp = 2;
					return;
				}
				if (abs(x - n.bx) < M && abs(y - n.by) < M) {
					dragging = p;
					dragp = 3;
					return;
				}
			}
			dragging = null;
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			dodrag(e.getX(), e.getY(), e.isShiftDown());
			if (e.getButton() == MouseEvent.BUTTON1) {
				dragging = null;
			}
			if (e.getButton() == MouseEvent.BUTTON3) {
				if (dragging != null) {
					if (dragging.next != null && dragging.prev != null) {
						dragging.prev.next = dragging.next;
						dragging.next.prev = dragging.prev;
						dragging.next.timea = dragging.timea;
						dragging.next.ay = dragging.ay;
						dragging.next.py = dragging.py;
					} else if (dragging.prev == null && dragging.next != null) {
						if (dragp == 3) {
							dragging.next.ay = dragging.ay;
							dragging.next.py = dragging.py;
							dragging.next.timea = dragging.timea;
						}
						pts = dragging.next;
						pts.prev = null;
					} else if (dragging.prev != null && dragging.next == null) {
						dragging.prev.next = null;
					}
					dragging = null;
				} else {
					CP p;
					float t = (lastx + scroll.getValue()) / getZoom();
					if (t < pts.timea) {
						p = new CP(t, pts.timea);
						p.next = pts;
						pts.prev = p;
						p.ay = p.py = p.qy = p.by = pts.ay;
						pts = p;
					} else {
						for (p = pts;; p = p.next) {
							if (t >= p.timea && t < p.timeb) {
								CP n = new CP(t, p.timeb);
								n.by = p.by;
								n.qy = p.qy;
								float v = rawval(t);
								n.py = v;
								n.ay = v;
								n.next = p.next;
								n.prev = p;
								p.timeb = t;
								p.qy = v;
								p.by = v;
								p.next.prev = n;
								p.next = n;
								break;
							}
							if (p.next == null) {
								p.next = new CP(p.timeb, p.timeb + 5);
								p.next.ay = p.next.py = p.next.qy = p.next.by = p.by;
								p.next.py += p.by - p.qy;
								p.next.prev = p;
								break;
							}
						}
					}
				}
				repaintAll();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
		}

		void dodrag(int x, int y, boolean shift)
		{
			int h = this.getHeight();
			x += scroll.getValue();
			if (dragging == null) {
				return;
			}
			float f;
			Nub n = new Nub();
			n.from(dragging, h);
			switch (dragp) {
			case 0:
				f = dragging.py - dragging.ay;
				dragging.ay = (float) y / h;
				dragging.py = f + dragging.ay;
				dragging.timea = x / getZoom();
				if (shift) {
					dragging.py = dragging.ay;
					dragging.qy = dragging.ay;
					dragging.by = dragging.ay;
					if (dragging.next != null) {
						dragging.next.ay = dragging.ay;
						dragging.next.py = dragging.ay;
					}
				}
				if (dragging.prev != null) {
					dragging.prev.by = dragging.ay;
					dragging.prev.qy = dragging.ay - (dragging.py - dragging.ay);
					dragging.prev.timeb = dragging.timea;
				}
				break;
			case 1:
				dragging.py = (float) y / h;
				if (dragging.prev != null) {
					dragging.prev.qy = dragging.ay - (dragging.py - dragging.ay) * (dragging.prev.timeb - dragging.prev.timea) / (dragging.timeb - dragging.timea);
				}
				break;
			case 2:
				dragging.qy = (float) y / h;
				if (dragging.next != null) {
					dragging.next.py = dragging.by - (dragging.qy - dragging.by) * (dragging.next.timeb - dragging.next.timea) / (dragging.timeb - dragging.timea);
				}
				break;
			case 3:
				f = dragging.qy - dragging.by;
				dragging.by = (float) y / h;
				dragging.qy = f + dragging.by;
				dragging.timeb = x / getZoom();
				if (dragging.next != null) {
					dragging.next.ay = dragging.by;
					if (shift) {
						dragging.next.py = dragging.by;
						dragging.next.qy = dragging.by;
						dragging.next.by = dragging.by;
						if (dragging.next.next != null) {
							dragging.next.next.ay = dragging.by;
							dragging.next.next.py = dragging.by;
						}
					} else {
						dragging.next.py = dragging.by - (dragging.qy - dragging.by);
					}
					dragging.next.timea = dragging.timeb;
				}
				break;
			}
			repaintAll();
		}

		void repaintAll()
		{
			for (Editor e : editors) {
				e.repaint();
			}
		}

		@Override
		public void run()
		{
			this.repaint();
			SwingUtilities.invokeLater(this);
		}

		float rawval(float t)
		{
			CP p;
			for (p = this.pts; p.next != null; p = p.next) {
				if (p.timea <= t && t < p.timeb) {
					break;
				}
			}
			float a = (t - p.timea) / (p.timeb - p.timea);
			float b = 1 - a;
			return b*b*b*p.ay+3f*b*b*a*p.py+3f*b*a*a*p.qy+a*a*a*p.by;
		}

		float val(float t) {
			return min + (max - min) * rawval(t);
		}
	}

	public static class Nub {
		public float ax, ay, bx, by, px, py, qx, qy;
		public void from(CP p, int h) {
			ax = p.timea * getZoom();
			ay = p.ay * h;
			bx = p.timeb * getZoom();
			by = p.by * h;
			px = ax + 30;
			py = p.py * h;
			qx = bx - 30;
			qy = p.qy * h;
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == time) {
			ShaderThing2.Var v = ShaderThing2.vars[0];
			v.slider.setValue(1000);
			v.max = time.getValue() / 100f;
			v.txtMax.setText(String.format("%.1f", v.max));
			scroll.setValue((int) (v.max * getZoom()) - editors[0].getWidth() / 2); // no need to clamp lol?
			ShaderThing2.varUpdateVal(v);
		}
		for (Editor ed : editors) {
			ed.repaint();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		action(((JButton) e.getSource()).getText());
	}

	void action(String name)
	{
		File file = ShaderThing2.file;
		if (file == null) {
			return;
		}
		if ("save".equals(name) && ShaderThing2.file != null) {
			ArrayList<String> lines = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				if (i != 0) {
					lines.add("-");
				}
				for (CP p = editors[i].pts; p != null; p = p.next) {
					lines.add(String.format("%f", p.timea));
					lines.add(String.format("%f", p.timeb));
					lines.add(String.format("%f", p.ay));
					lines.add(String.format("%f", p.py));
					lines.add(String.format("%f", p.qy));
					lines.add(String.format("%f", p.by));
				}
			}
			File f = new File(file.getParentFile(), file.getName() + ".pos");
			try {
				Files.write(f.toPath(), lines, CREATE, TRUNCATE_EXISTING);
			} catch (IOException e1) {
				ShaderThing2.errDlg(this, e1, "failed to write .pos file");
			}
			lines.clear();
			for (int i = 0; i < 5; i++) {
				Editor e = editors[i];
				lines.add(String.format("const float[] a%s=float[](", e.getName()));
				for (CP p = e.pts; p != null; p = p.next) {
					lines.add(String.format(
						"%s,%s,%s,%s,%s,%s,",
						flt(p.timea),
						flt(p.timeb),
						flt(p.ay),
						flt(p.py),
						flt(p.qy),
						flt(p.by)
					));
				}
				lines.add("-1);");
			}
			f = new File(file.getParentFile(), file.getName() + ".pos.c");
			try {
				Files.write(f.toPath(), lines, CREATE, TRUNCATE_EXISTING);
			} catch (IOException e1) {
				ShaderThing2.errDlg(this, e1, "failed to write .pos.c file");
			}
		} else if ("load".equals(name)) {
			File f = new File(file.getParentFile(), file.getName() + ".pos");
			try {
				List<String> lines = Files.readAllLines(f.toPath());
				CP prev = null;
				Editor e = editors[0];
				int ei = 0;
				for (int i = 0; i < lines.size(); i++) {
					if ("-".equals(lines.get(i))) {
						e = editors[++ei];
						e.pts = null;
						prev = null;
						continue;
					}
					CP p = new CP(0, 0);
					if (prev == null) {
						e.pts = p;
					} else {
						prev.next = p;
						p.prev = prev;
					}
					p.timea = Float.parseFloat(lines.get(i));
					p.timeb = Float.parseFloat(lines.get(i + 1));
					p.ay = Float.parseFloat(lines.get(i + 2));
					p.py = Float.parseFloat(lines.get(i + 3));
					p.qy = Float.parseFloat(lines.get(i + 4));
					p.by = Float.parseFloat(lines.get(i + 5));
					i+=5;
					prev = p;
				}
			} catch (Exception e1) {
				ShaderThing2.errDlg(this, e1, "failed to read");
			}
		} else {
			JOptionPane.showMessageDialog(this, "unknown button", "tit", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static String flt(float f) {
		String s = String.format("%.2f", f);
		while (s.charAt(0) == '0') s = s.substring(1);
		while (s.charAt(s.length()-1) == '0') s = s.substring(0, s.length() - 1);
		if (s.length() == 1 && s.charAt(0) == '.') {
			return ".0";
		}
		return s;
	}
}
