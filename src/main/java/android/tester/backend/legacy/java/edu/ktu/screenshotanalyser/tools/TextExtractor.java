package android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.tools;

import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.utils.ImageUtils;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TextExtractor {

  static {
    // Ensure OpenCV is loaded before any OpenCV classes are used
    // Note: You must have the opencv dll/so in your java.library.path
    try {
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native code library failed to load. \n" + e);
      // Fallback or specific handling if needed, usually Nuget/Maven packages handle this if configured correctly
      // otherwise, -Djava.library.path=path/to/opencv/lib must be set in launch config
    }
  }

  private final ITesseract tesseract;
  private final float confidenceLevel;

  // Hardcoded path based on user requirement
  private static final String GOCR_PATH = "C:\\gocr\\gocr048.exe";

//  public TextExtractor(float confidenceLevel, String language) {
//    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Tesseract.class.getName());
//    logger.setLevel(ch.qos.logback.classic.Level.ALL);
//
//    //logger.getRootLogger().setLevel(Level.OFF);
//
//    this.confidenceLevel = confidenceLevel;
//
//    this.tesseract = new Tesseract();
//    this.tesseract.setDatapath(new File("./tessdata_best").getAbsolutePath()); // TODO: folder in app settings
//    this.tesseract.setLanguage(language);
//
//
//    List<String> config = new ArrayList<>();
//
//    String[] f = new String[]{"load_system_dawg", "load_freq_dawg",
//      "load_punc_dawg",
//      "load_number_dawg",
//      "load_unambig_dawg",
//      "load_bigram_dawg",
//      "load_fixed_length_dawgs",};
//
//    tesseract.setTessVariable("debug_file", "e:\\1\\tesseract.log");
//  }

//  public TextExtractor(float confidenceLevel, String language) {
//    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Tesseract.class.getName());
//    logger.setLevel(ch.qos.logback.classic.Level.ALL);
//
//    this.confidenceLevel = confidenceLevel;
//
//    this.tesseract = new Tesseract();
//    // Changed to standard 'tessdata' and checking existence
//    File tessDataFolder = new File("./tessdata");
//    if (!tessDataFolder.exists()) {
//      tessDataFolder = new File("./tessdata_best");
//    }
//
//    // Tesseract typically uses ISO 639-2/T 3-letter codes (e.g., "eng" instead of "en")
//    String tessLanguage = language;
//    if ("en".equalsIgnoreCase(language)) {
//      tessLanguage = "eng";
//    }
//
//    // Fallback if folder exists, otherwise let Tesseract throw its own error or use default system install
//    if (tessDataFolder.exists()) {
//      this.tesseract.setDatapath(tessDataFolder.getAbsolutePath());
//    }
//
//    this.tesseract.setLanguage(tessLanguage);
//
//    List<String> config = new ArrayList<>();
//
//    String[] f = new String[]{"load_system_dawg", "load_freq_dawg",
//      "load_punc_dawg",
//      "load_number_dawg",
//      "load_unambig_dawg",
//      "load_bigram_dawg",
//      "load_fixed_length_dawgs",};
//
//    // Disabled hardcoded E drive path for logging
//    // tesseract.setTessVariable("debug_file", "e:\\1\\tesseract.log");
//  }

  public TextExtractor(float confidenceLevel, String language) {
    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Tesseract.class.getName());
    logger.setLevel(ch.qos.logback.classic.Level.ALL);

    this.confidenceLevel = confidenceLevel;

    this.tesseract = new Tesseract();
    File tessDataFolder = new File("./tessdata");
    if (!tessDataFolder.exists()) {
      tessDataFolder = new File("./tessdata_best");
    }

    String tessLanguage = language;
    if ("en".equalsIgnoreCase(language)) {
      tessLanguage = "eng";
    }

    if (tessDataFolder.exists()) {
      this.tesseract.setDatapath(tessDataFolder.getAbsolutePath());
    }

    this.tesseract.setLanguage(tessLanguage);
  }

  public String extract(BufferedImage image) {
    var result = "";

    try {
      var os = new ByteArrayOutputStream();
      ImageIO.write(image, "png", os);

//      var process = Runtime.getRuntime().exec("gocr.exe", new String[]{"PYTHONIOENCODING=utf8"}, null);
      // UPDATED: Using specific path for gocr
      var process = Runtime.getRuntime().exec(new String[]{GOCR_PATH}, new String[]{"PYTHONIOENCODING=utf8"});

      process.getOutputStream().write(os.toByteArray());
      process.getOutputStream().close(); // Important to close stream for input to finish

      try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line = null;

        while ((line = reader.readLine()) != null) {
          result += line;
        }
      }

      try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
        String line = null;

        while ((line = reader.readLine()) != null) {
          result += line;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    // System.out.println("[" + result + "]");

    return result;
  }

  public String extract(BufferedImage image, Rect bounds, Predicate<String> accept) {
    if (null == image) {
      return "";
    }

    Mat sourceImage = null;

    try {
      String result = clean(this.tesseract.doOCR(image, SystemUtils.toRectangle(bounds)));

      if (accept.test(result)) {
        return result;
      }

      if (null != bounds) {
        try {
          image = image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        } catch (java.awt.image.RasterFormatException ex) {
          ex.printStackTrace();

          return "";
        }
      }

      sourceImage = ImageUtils.bufferedImageToMat(image);

      if (isTooSmall(sourceImage)) {
        //Imgcodecs.imwrite("d:/s1.png", sourceImage);

        Mat scaledImage = new Mat();

        double scale = 50.0 / (double) sourceImage.rows();

        Imgproc.resize(sourceImage, scaledImage, new Size(), scale, scale, Imgproc.INTER_CUBIC);

        //Imgcodecs.imwrite("d:/s2.png", scaledImage);

        sourceImage = scaledImage;
      }

      Mat grayScaleImage = convertToGrayScale(sourceImage);

      grayScaleImage = convertToDarkLettersOnWhite(grayScaleImage);

      Mat gaussianBlurredImage = new Mat();
      Imgproc.GaussianBlur(grayScaleImage, gaussianBlurredImage, new Size(3, 3), 0);

      result = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(gaussianBlurredImage)));

      if (accept.test(result)) {
        return result;
      }

      Mat adaptiveThresholdImage = new Mat();
      Imgproc.adaptiveThreshold(gaussianBlurredImage, adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 4);

      result = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(adaptiveThresholdImage)));

      if (accept.test(result)) {
        return result;
      }


    } catch (TesseractException ex) {
      ex.printStackTrace();

      try {
        ImageIO.write(image, "jpg", new File("d:\\image.jpg"));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }


    } catch (Throwable ex) {
      ex.printStackTrace();

      return "";
    }

    return "";
  }


  public String extract(File imageFile) {
    try {
      Mat sourceImage = Imgcodecs.imread(imageFile.getAbsolutePath());
      Mat grayScaleImage = new Mat();

      Imgproc.cvtColor(sourceImage, grayScaleImage, Imgproc.COLOR_BGR2GRAY);

      Mat gaussianBlurredImage = new Mat();
      Imgproc.GaussianBlur(grayScaleImage, gaussianBlurredImage, new Size(3, 3), 0);

      Mat adaptiveThresholdImage = new Mat();
      Imgproc.adaptiveThreshold(gaussianBlurredImage, adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 4);
      //Imgcodecs.imwrite("e:/4.png", adaptiveThresholdImage);

      String result1 = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(adaptiveThresholdImage)));

      Core.bitwise_not(gaussianBlurredImage, gaussianBlurredImage);

      Imgproc.adaptiveThreshold(gaussianBlurredImage, adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 4);
      //Imgcodecs.imwrite("e:/5.png", adaptiveThresholdImage);

      String result2 = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(adaptiveThresholdImage)));

      Size sz = new Size(sourceImage.width() * 2, sourceImage.height() * 2);
      Imgproc.resize(sourceImage, sourceImage, sz);

      Imgproc.cvtColor(sourceImage, grayScaleImage, Imgproc.COLOR_BGR2GRAY);
      Imgproc.GaussianBlur(grayScaleImage, gaussianBlurredImage, new Size(3, 3), 0);
      Imgproc.adaptiveThreshold(gaussianBlurredImage, adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 4);
      //Imgcodecs.imwrite("e:/14.png", adaptiveThresholdImage);

      String result3 = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(adaptiveThresholdImage)));

      Core.bitwise_not(gaussianBlurredImage, gaussianBlurredImage);

      Imgproc.adaptiveThreshold(gaussianBlurredImage, adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 4);
      //Imgcodecs.imwrite("e:/15.png", adaptiveThresholdImage);

      String result4 = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(adaptiveThresholdImage)));

      return result1 + " " + result2 + " " + result3 + " " + result4;
    } catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  private String clean(String text) {
    if (null == text) {
      text = "";
    }

    return text.trim();
  }

  public String extract(BufferedImage image, Rect bounds) {
    if (null == image) {
      return "";
    }

    try {

      if (null != bounds) {
        try {
          image = image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        } catch (java.awt.image.RasterFormatException e) {
          e.printStackTrace();
        }
      }

      Mat sourceImage = ImageUtils.bufferedImageToMat(image);

      if (isTooSmall(sourceImage)) {

        Mat scaledImage = new Mat();

        double scale = 50.0 / (double) sourceImage.rows();

        Imgproc.resize(sourceImage, scaledImage, new Size(), scale, scale, Imgproc.INTER_CUBIC);


        sourceImage = scaledImage;
      }

      Mat grayScaleImage = convertToGrayScale(sourceImage);

      // grayScaleImage = convertToDarkLettersOnWhite(sourceImage); // original likely meant grayScaleImage


      Mat gaussianBlurredImage = new Mat();
      Imgproc.GaussianBlur(grayScaleImage, gaussianBlurredImage, new Size(3, 3), 0);

      Mat adaptiveThresholdImage = new Mat();
      Imgproc.adaptiveThreshold(gaussianBlurredImage, adaptiveThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 4);

      String result1 = clean(this.tesseract.doOCR(ImageUtils.matToBufferedImage(adaptiveThresholdImage)));

      return result1;// + " " + result2 + " " + result3 + " " + result4;


    } catch (Throwable e) {
      e.printStackTrace();

      return "";
    }
  }


  public String extract(File imageFile, Rect area) {
    if ((area.width <= 2) || (area.height <= 2) || (area.x < 0) || (area.y < 0)) {
      return "";
    }

    Mat img2 = new Mat();
    img2 = Imgcodecs.imread(imageFile.getAbsolutePath());
    //Imgcodecs.imwrite("e:/11.png", img2);

    Mat imgGray = new Mat();
    Imgproc.cvtColor(img2, imgGray, Imgproc.COLOR_BGR2GRAY);
    //Imgcodecs.imwrite("e:/22.png", imgGray);

    Mat imgGaussianBlur = new Mat();
    Imgproc.GaussianBlur(imgGray, imgGaussianBlur, new Size(3, 3), 0);
    //Imgcodecs.imwrite("e:/33.png", imgGaussianBlur);

    Mat imgAdaptiveThreshold = new Mat();
    Imgproc.adaptiveThreshold(imgGaussianBlur, imgAdaptiveThreshold, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);

    BufferedImage image = ImageUtils.matToBufferedImage(imgAdaptiveThreshold);

    BufferedImage subImage = image.getSubimage(area.x, area.y, area.width, area.height);

    if (null == subImage) {
      return "";
    }

    StringBuilder result = new StringBuilder();

    try {
      for (Word word : this.tesseract.getWords(subImage, 0)) {

        if (word.getConfidence() > this.confidenceLevel) {
          result.append(" " + word.getText());
        }
      }
    } catch (Throwable ex) {
      ex.printStackTrace();
    }

    return result.toString().trim();
  }


  public record ExtractedText(Word[] orginalWords, String text, Rect area) {
  }

  public static class TextExtractResponse {
    final List<ExtractedText> extractedTexts = new ArrayList<>();
    private final float confidenceLevel;

    public TextExtractResponse(List<ExtractedText> extractedTexts, float confidenceLevel) {
      this.confidenceLevel = confidenceLevel;
      this.extractedTexts.addAll(extractedTexts);
    }

    public List<ExtractedText> getExtractedTexts() {
      return extractedTexts;
    }
  }

  public static class TextExtractRequest {
    private final File file;
    private final List<Rect> bounds = new ArrayList<>();

    public List<Rect> getBounds() {
      return bounds;
    }

    public TextExtractRequest(File file, List<Rect> bounds) {
      this.file = file;
      this.bounds.addAll(bounds);
    }

    public TextExtractRequest(File file) {
      this.file = file;
    }

    public File getFile() {
      return file;
    }
  }


  private static boolean isTooSmall(Mat image) {
    return image.rows() <= 50;
  }

  private static Mat convertToDarkLettersOnWhite(Mat sourceImage) {
    if (hasDarkMode(sourceImage)) {
      Mat image = new Mat();

      Core.bitwise_not(sourceImage, image);

      return image;
    } else {
      return sourceImage;
    }
  }

  private static Mat convertToGrayScale(Mat sourceImage) {
    Mat image = new Mat();

    Imgproc.cvtColor(sourceImage, image, Imgproc.COLOR_BGR2GRAY);

    return image;
  }

  private static boolean hasDarkMode(Mat sourceImage) {
    Mat image = new Mat();

    Imgproc.blur(sourceImage, image, new Size(5, 5));

    return Core.mean(image).val[0] < 127;
  }

  private static Mat getImage(File imageFile) {
    if (null == imageFile) {
      return null;
    }

    try {
      BufferedImage image = ImageIO.read(imageFile);

      return ImageUtils.bufferedImageToMat(image);
    } catch (IOException ex) {
      ex.printStackTrace();

      return null;
    }
  }
}
