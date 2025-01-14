package sketchify;

import org.jspace.RemoteSpace;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class Draw {
    double x,y;
    String actionType;

    public Draw(double x, double y, String actionType) {
        this.x = x;
        this.y = y;
        this.actionType = actionType;
    }

    public void isPressed(Canvas canvas, GraphicsContext gc, RemoteSpace drawSpace) {
        canvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
            try {
                drawSpace.put("draw", e.getX(), e.getY(), "start");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
        });
    }

    public void isDragged(Canvas canvas, GraphicsContext gc, RemoteSpace drawSpace) {
        canvas.setOnMouseDragged(e -> {
            double x = e.getX();
            double y = e.getY();
        
            gc.lineTo(x, y);
            gc.stroke();
            try {
                drawSpace.put("draw", x, y, "draw");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
        });
    }

}