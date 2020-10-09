package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.FileExceptionEnum;
import com.leyou.common.exception.LyException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-17 10:09
 **/
@Service
public class UploadService {

    @Value("${ly.upload.baseUrl}")
    public String baseUrl;

    @Autowired
    private FastFileStorageClient storageClient;

    private static final List<String> ALLOW_FILE_TYPES = Arrays.asList("image/png", "image/jpeg");

    public String uploadImage(MultipartFile file) {
        try {
            // 对文件类型校验
            if (!ALLOW_FILE_TYPES.contains(file.getContentType())) {
                throw new LyException(FileExceptionEnum.FILE_TYPE_ERROR);
            }
            // 校验文件内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new LyException(FileExceptionEnum.FILE_CONTENT_ERROR);
            }

          /*  // 准备文件夹
            File destDir = new File("D:\\heima37\\nginx-1.12.2\\html");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            // 保存

            file.transferTo(new File(destDir, file.getOriginalFilename()));*/

            // 上传到FastDFS
            String fileExtName = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), fileExtName, null);
            // 构建url地址
            String url = baseUrl + storePath.getFullPath();
            return url;
        } catch (IOException e) {
            throw new LyException(FileExceptionEnum.FILE_UPLOAD_FAIL);
        }
    }
}
