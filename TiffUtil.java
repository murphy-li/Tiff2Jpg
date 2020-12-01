import com.alibaba.fastjson.JSON;
import com.sun.glass.ui.Size;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TiffUtil {
    public static void main(String[] args) throws IOException {
        System.out.println(transferTiff2Image("C:/Users/xxx/Desktop/44.tif", "1.jpg"));;
    }


    /**
     * 判断文件路径是否为空
     *
     * @param filePath 文件路径
     */
    public static void checkFilePathNull(String filePath) {
        if (filePath == null || "".equalsIgnoreCase(filePath.trim())) {
            throw new RuntimeException("文件路径不能为空");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger("Tiff2Jpeg");
    private static final String TIFF = "tiff";
    private static final String TIF = "tif";

    /**
     * @param localFilePath  tiff文件的本地文件路径（绝对路径）
     * @param savingFilePath tiff文件转成雪碧图之后的输出路径（绝对路径）
     * @return 雪碧图中各个坐标之间的关系
     */
    public static String transferTiff2Image(String localFilePath, String savingFilePath) throws IOException {

        checkFilePathNull(localFilePath);
        checkFilePathNull(savingFilePath);
        String format = localFilePath.substring(localFilePath.lastIndexOf(".") + 1);
        InputStream inputStream = new FileInputStream(localFilePath);
        if (TIFF.equalsIgnoreCase(format) || TIF.equalsIgnoreCase(format)) {

            /**
             * 读取文件
             */
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", inputStream, null);
            /**
             * 获得页数
             */
            int pageNum = dec.getNumPages();
            logger.info("当前tiff文件为" + localFilePath + "， 一共" + pageNum + "页");
            List<BufferedImage> bufferedImages = new ArrayList<>();
            for (int i = 0; i < pageNum; i++) {
                RenderedImage page = dec.decodeAsRenderedImage(i);
                BufferedImage bufferedImage = new BufferedImage(page.getWidth(), page.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                bufferedImage.setData(page.getData());
                bufferedImages.add(bufferedImage);
            }
            Size size = resolveSize(bufferedImages);
            // 相比TYPE_INT_RGB，TYPE_3BYTE_BGR更节省空间
            BufferedImage sprite = new BufferedImage(size.width, size.height, BufferedImage.TYPE_3BYTE_BGR);
            // 每一个图片的坐标信息
            List<Map<String, Object>> coordinateOfPages = new ArrayList<>();

            int top = 0;
            int i = 0;
            for (BufferedImage bufferedImage : bufferedImages) {
                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();
                //合成到大图
                composeImage(sprite, bufferedImage, top);
                logger.info("正在处理第" + i + "页，宽度为：" + width + "，高度为：" + height +"， 一共" + pageNum + "页");

                // 记录位置信息
                Map<String, Object> coordinateOfPage = new HashMap<>();
                coordinateOfPage.put("top", top);
                coordinateOfPage.put("w", width);
                coordinateOfPage.put("h", height);
                coordinateOfPage.put("no", i++);
                // 这里不记录fileStoreId
                // connectMsg.put("id", "");
                coordinateOfPages.add(coordinateOfPage);
                top += height;
            }


            // 图片位置信息最外层map,包含list全部位置和总尺寸信息
            Map<String, Object> allMsg = new HashMap<>();
            allMsg.put("pos", coordinateOfPages);
            allMsg.put("spriteW", size.width);
            allMsg.put("spriteH", size.height);
            ImageIO.write(sprite, "jpeg", new File(savingFilePath));
            return  JSON.toJSONString(allMsg);
//            return new SpriteInfo(savingFilePath, size.width, size.height, coordinateOfPages);
        } else {
            throw new RuntimeException("格式不是tiff或者tif");
        }
    }

    /**
     * 雪碧图信息包装类
     */
    public static class SpriteInfo {
        /**
         * 雪碧图保存路径
         */
        String filePath;
        /**
         * 雪碧图宽度
         */
        int spriteW;
        /**
         * 雪碧图高度
         */
        int spriteH;
        /**
         * 雪碧图尺寸Map集合
         */
        List<Map<String, Object>> sizeInfoOfPages;

        public SpriteInfo(String filePath, int spriteW, int spriteH, List<Map<String, Object>> sizeInfoOfPages) {
            this.filePath = filePath;
            this.spriteW = spriteW;
            this.spriteH = spriteH;
            this.sizeInfoOfPages = sizeInfoOfPages;
        }

        /**
         *
         * @return 雪碧图保存路径
         */
        public String getFilePath() {
            return filePath;
        }

        /**
         *
         * @return 雪碧图宽度
         */
        public int getSpriteW() {
            return spriteW;
        }

        /**
         * 雪碧图高度
         * @return
         */
        public int getSpriteH() {
            return spriteH;
        }

        /**
         *
         * @return 雪碧图尺寸集合
         */
        public List<Map<String, Object>> getSizeInfoOfPages() {
            return sizeInfoOfPages;
        }

        @Override
        public String toString() {
            return "SpriteImage{" +
                    "filePath='" + filePath + '\'' +
                    ", spriteW=" + spriteW +
                    ", spriteH=" + spriteH +
                    ", sizeInfoOfPages=" + sizeInfoOfPages +
                    '}';
        }
    }

    /**
     * 根据每一页的大小找出雪碧图的大小
     *
     * @param bufferedImages
     * @return
     */
    private static Size resolveSize(List<BufferedImage> bufferedImages) {

        // 获取分页图及总大小
        int maxW = 0, totalH = 0;
        for (BufferedImage bufferedImage : bufferedImages) {
            int w = bufferedImage.getWidth(),
                    h = bufferedImage.getHeight();
            maxW = Math.max(maxW, w);
            totalH += h;
        }
        return new Size(maxW, totalH);
    }

    /**
     * 将图片合成到雪碧图中
     *
     * @param target 最终的雪碧图
     * @param in     输入的图片
     * @param top    左上角的坐标
     */
    private static void composeImage(BufferedImage target, BufferedImage in, int top) {
        int width = in.getWidth(),
                height = in.getHeight();
        int[] imgIntArr = new int[width * height];
        imgIntArr = in.getRGB(0, 0, width, height, imgIntArr, 0, width);
        target.setRGB(0, top, width, height, imgIntArr, 0, width);
    }
}