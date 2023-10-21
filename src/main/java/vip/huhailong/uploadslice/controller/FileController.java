package vip.huhailong.uploadslice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 胡编乱码
 * 文件接口
 */
@RestController
@RequestMapping("/file")
public class FileController {

  private static  final Logger logger = LoggerFactory.getLogger(FileController.class);

  @Value("${slice.saveDir}")
  private String saveDir;

  private static final Map<String, Integer> fileCountMap = new ConcurrentHashMap<>();

  /**
   * 分片上传接口
   * @param file 分片文件
   * @param fileName 文件名称
   * @param fileMd5 文件MD5值
   * @param chunkCount 分片总数量
   * @param currentIndex 当前分片索引
   */
  @PostMapping("/uploadBySlice")
  public float upload(MultipartFile file, String fileName, String fileMd5, Integer chunkCount, Integer currentIndex){
    String tempFilePath = saveDir + File.separator + fileMd5 + ".tmp";
    checkFilePath();
    try(RandomAccessFile accessFile = new RandomAccessFile(tempFilePath,"rw")){
      accessFile.seek(currentIndex);
      accessFile.write(file.getBytes());
      fileCountMap.put(fileMd5, fileCountMap.getOrDefault(fileMd5,0)+1);
      if(Objects.equals(fileCountMap.get(fileMd5), chunkCount)){
        File tempFile = new File(tempFilePath);
        File finallyFile = new File(saveDir + File.separator + fileName);
        tempFile.renameTo(finallyFile);
        tempFile.delete();
        logger.info("上传完成：{}",saveDir+File.separator+fileName);
        fileCountMap.remove(fileMd5);
        return 1.0f;
      }else{
        logger.info("{}-{}件上传中",fileMd5,currentIndex);
        return (float) fileCountMap.get(fileMd5) / chunkCount;
      }
    }catch (Exception e){
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private void checkFilePath(){
    File file = new File(saveDir);
    if(!file.exists()){
      file.mkdirs();
    }
  }
}
