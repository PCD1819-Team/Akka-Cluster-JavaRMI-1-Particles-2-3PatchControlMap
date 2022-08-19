package it.unibo.isi.pcd.model;

import com.sun.javafx.geom.Vec2d;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class Particle {
	private final double alfaConst;
	private final double mConst;
	private Point2D currentPosition;
	private Vec2d currentSpeed;
	private Vec2d currentForce;
	private final Color color;

	public Particle(final double alfaConst, final double mConst, final Point2D startingPosition,
			final Vec2d startingSpeed, final Vec2d startingForce, final Color color) {
		this.alfaConst = alfaConst;
		this.mConst = mConst;
		this.currentPosition = startingPosition;
		this.currentForce = startingForce;
		this.currentSpeed = startingSpeed;
		this.color = color;
	}

	public Particle(final Particle p) {
		this.alfaConst = p.alfaConst;
		this.mConst = p.mConst;
		this.currentForce = p.currentForce;
		this.currentPosition = p.currentPosition;
		this.currentSpeed = p.currentSpeed;
		this.color = p.color;
	}

	public double getAlfaConst() {
		return this.alfaConst;
	}

	public double getmConst() {
		return this.mConst;
	}

	public Point2D getPosition() {
		return this.currentPosition;
	}

	public Vec2d getCurrentSpeed() {
		return this.currentSpeed;
	}

	public Vec2d getCurrentForce() {
		return this.currentForce;
	}

	public void updatePosition(final Point2D newPos) {
		this.currentPosition = newPos;
	}

	public void updateSpeed(final Vec2d newSpeed) {
		this.currentSpeed = newSpeed;
	}

	public void updateForce(final Vec2d newForce) {
		this.currentForce = newForce;
	}

	public Color getColor() {
		return this.color;
	}

}
