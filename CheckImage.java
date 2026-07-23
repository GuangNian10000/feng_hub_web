import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CheckImage {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "composeApp/src/commonMain/composeResources/drawable/ic_face.png",
            "composeApp/src/commonMain/composeResources/drawable/ic_control.png",
            "composeApp/src/commonMain/composeResources/drawable/ic_device_connection.png"
        };
        for(String path : files) {
            File f = new File(path);
            if(!f.exists()) {
                System.out.println(path + " does not exist.");
                continue;
            }
            BufferedImage img = ImageIO.read(f);
            int width = img.getWidth();
            int height = img.getHeight();
            int minX = width, minY = height, maxX = 0, maxY = 0;
            boolean hasTransparent = false;
            for(int y=0; y<height; y++) {
                for(int x=0; x<width; x++) {
                    int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                    if(alpha > 10) {
                        if(x < minX) minX = x;
                        if(y < minY) minY = y;
                        if(x > maxX) maxX = x;
                        if(y > maxY) maxY = y;
                    } else {
                        hasTransparent = true;
                    }
                }
            }
            System.out.println(f.getName() + " -> Size: " + width + "x" + height + 
                " | Content Bounding Box: [" + minX + ", " + minY + " to " + maxX + ", " + maxY + "]" +
                " | Width: " + (maxX - minX + 1) + ", Height: " + (maxY - minY + 1) +
                " | Has Transparent: " + hasTransparent);
        }
    }
}
