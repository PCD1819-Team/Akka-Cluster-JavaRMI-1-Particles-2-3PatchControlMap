package it.unibo.isi.pcd.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.sun.javafx.geom.Vec2d;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public final class FunctionalModel {
  public static double deltaT = 0.3;
  public static double k = 0.5;
  public static double kAttr = 0.1;

  public static List<Point2D> getPositions(final List<Particle> particles) {
    return particles.stream().map(p -> p.getPosition()).collect(Collectors.toList());
  }

  public static Map<Integer, Particle> computeParticles(final int lowerBound, final int upperBound,
      final List<Particle> particles) {
    final Map<Integer, Particle> computedParticles = new HashMap<>();
    for (int i = lowerBound; i <= upperBound; i++) {
      computedParticles.put(i, FunctionalModel.computeParticle(i, particles));

    }
    return computedParticles;
  }

  public static Particle generateRandomParticle(final int xBound, final int yBound) {
    final Random random = new Random();
    final Color color = Color.rgb(ThreadLocalRandom.current().nextInt(0, 256),
        ThreadLocalRandom.current().nextInt(0, 256), ThreadLocalRandom.current().nextInt(0, 256));
    return new Particle(ThreadLocalRandom.current().nextDouble(1, 10),
        ThreadLocalRandom.current().nextDouble(0.1, 0.99),
        new Point2D(((xBound / 2) + random.nextInt((xBound / 2) + 1 + (xBound / 2))) - (xBound / 2),
            ((yBound / 2) + (random.nextInt((yBound / 2) + 1 + (yBound / 2)))) - (yBound / 2)),
        new Vec2d(0, 0), new Vec2d(0, 0), color);

  }

  private static Particle computeParticle(final int i, final List<Particle> particles) {
    final Particle p = new Particle(particles.get(i));
    final Vec2d newForce = FunctionalModel.computeNewForce(i, particles);
    p.updateForce(newForce);
    p.updatePosition(FunctionalModel.computeNewPosition(p));
    p.updateSpeed(FunctionalModel.computeNewSpeed(p, newForce));
    return p;

  }

  private static Point2D computeNewPosition(final Particle p) {
    final Vec2d currentSpeed = p.getCurrentSpeed();
//    System.out.println("---------POS X" + p.getPosition().getX());
//    System.out.println("---------POS Y" + p.getPosition().getY());
    return new Point2D(p.getPosition().getX() + (currentSpeed.x * FunctionalModel.deltaT),
        p.getPosition().getY() + (currentSpeed.y * FunctionalModel.deltaT));
  }

  private static Vec2d computeNewSpeed(final Particle p, final Vec2d newForce) {
//    System.out.println("------SPEED X" + p.getCurrentSpeed().x);
//    System.out.println("------SPEED Y" + p.getCurrentSpeed().y);

    return new Vec2d(
        p.getCurrentSpeed().x + ((FunctionalModel.deltaT * newForce.x) / p.getmConst()),
        p.getCurrentSpeed().y + ((FunctionalModel.deltaT * newForce.y) / p.getmConst()));

  }

  private static Vec2d computeNewForce(final int partIndex, final List<Particle> parts) {
    final Particle partI = parts.get(partIndex);
    final Vec2d force = parts.get(partIndex).getCurrentForce();
    double dist = 0;
    double dx = 0;
    double dy = 0;
    double checkDx = 0;
    double checkDy = 0;

    double commonFormulaPart = 0;
    for (int j = 0; j < parts.size(); j++) {
      if (j != partIndex) {
        final Particle partJ = parts.get(j);
        checkDx = partI.getPosition().getX() - partJ.getPosition().getX();
        checkDy = partI.getPosition().getY() - partJ.getPosition().getY();
        dx = checkDx != 0.0 ? checkDx : 0.1;
        dy = checkDy != 0.0 ? checkDy : 0.1;
        dist = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));

        commonFormulaPart = (FunctionalModel.k * partI.getAlfaConst() * partJ.getAlfaConst())
            / Math.pow(dist, 3);

        force.x += (commonFormulaPart * dx);
        force.y += (commonFormulaPart * dy);
        force.y += 0;

      }
    }

    force.x = (force.x - (FunctionalModel.kAttr * partI.getCurrentSpeed().x)) <= 0 ? 0
        : force.x + (-FunctionalModel.kAttr * partI.getCurrentSpeed().x);
    force.y = (force.y - (FunctionalModel.kAttr * partI.getCurrentSpeed().y)) <= 0 ? 0
        : force.y + (-FunctionalModel.kAttr * partI.getCurrentSpeed().y);

//    System.out.println("---FORCE X" + force.x);
//    System.out.println("---FORCE Y" + force.y);
    return force;

  }

}