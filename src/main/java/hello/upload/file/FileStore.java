package hello.upload.file;

import hello.upload.domain.UploadFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FileStore {

    @Value("${file.dir}")
    private String fileDir;

    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    public List<UploadFile> storeFiles(List<MultipartFile> multipartFiles) throws IOException {
        List<UploadFile> storeFileResult = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            if (!multipartFile.isEmpty()) {
                //for문 돌리면서 서버에 파일 저장
                UploadFile uploadFile = storeFile(multipartFile);
                storeFileResult.add(uploadFile);
            }
        }
        return storeFileResult;
    }

    public UploadFile storeFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        //image.png

        //서버에 저장하는 파일명 정하기
        String storeFileName = createStoreFileName(originalFilename);
        //파일 저장하기(storeFileName 서버에 저장하는 파일이름)
        multipartFile.transferTo(new File(getFullPath(storeFileName)));

        return new UploadFile(originalFilename, storeFileName);

    }

    //서버에 저장하는 파일명 생성 함수
    private String createStoreFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        //확장자 가져오기
        String ext = extractExt(originalFilename);
        //서버에 저장하는 파일명
        return uuid + "." + ext;
    }

    //확장자 명 지정하는 함수
    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        //확장자 명
        return originalFilename.substring(pos + 1);
    }


}
