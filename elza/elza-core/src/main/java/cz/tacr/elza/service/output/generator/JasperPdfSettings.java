package cz.tacr.elza.service.output.generator;

/**
 * 
 * Example of JSON serialization:
 * {"evenPageOffsetX":100,"evenPageOffsetY":500,"oddPageOffsetX":-200,"oddPageOffsetY":-30}
 *
 */
public class JasperPdfSettings {
    Integer evenPageOffsetX;
    Integer evenPageOffsetY;
    Integer oddPageOffsetX;
    Integer oddPageOffsetY;

    public Integer getEvenPageOffsetX() {
        return evenPageOffsetX;
    }

    public void setEvenPageOffsetX(Integer evenPageOffsetX) {
        this.evenPageOffsetX = evenPageOffsetX;
    }

    public Integer getEvenPageOffsetY() {
        return evenPageOffsetY;
    }

    public void setEvenPageOffsetY(Integer evenPageOffsetY) {
        this.evenPageOffsetY = evenPageOffsetY;
    }

    public Integer getOddPageOffsetX() {
        return oddPageOffsetX;
    }

    public void setOddPageOffsetX(Integer oddPageOffsetX) {
        this.oddPageOffsetX = oddPageOffsetX;
    }

    public Integer getOddPageOffsetY() {
        return oddPageOffsetY;
    }

    public void setOddPageOffsetY(Integer oddPageOffsetY) {
        this.oddPageOffsetY = oddPageOffsetY;
    }
}
