package ru.nivshin.excelannotations.engine;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Investigated area
 */
@Data
@NoArgsConstructor
public class InvestigatedArea {

    private Integer upperLeftX;
    private Integer upperLeftY;
    private Integer bottomRightX;
    private Integer bottomRightY;

    private Map<Integer, Object> columnHeaders = new HashMap<>();

    public InvestigatedArea(final Integer upperLeftX, final Integer upperLeftY, final Integer bottomRightX,
                            final Integer bottomRightY) {
        this.upperLeftX = upperLeftX;
        this.upperLeftY = upperLeftY;
        this.bottomRightX = bottomRightX;
        this.bottomRightY = bottomRightY;
    }

    boolean isEmpty() {
        return upperLeftX == null;
    }

    void extendArea(final int xValue, final int yValue) {

        if (xValue < 0 || yValue < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative");
        }

        if (isEmpty()) {
            upperLeftX = xValue;
            bottomRightX = xValue;
            upperLeftY = yValue;
            bottomRightY = yValue;
        } else {
            if (upperLeftX > xValue) {
                upperLeftX = xValue;
            }
            if (xValue > bottomRightX) {
                bottomRightX = xValue;
            }
            if (upperLeftY > yValue) {
                upperLeftY = yValue;
            }
            if (yValue > bottomRightY) {
                bottomRightY = yValue;
            }
        }
    }
}
