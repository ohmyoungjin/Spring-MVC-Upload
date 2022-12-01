package hello.upload.controller;

import hello.upload.domain.Item;
import hello.upload.domain.ItemRepository;
import hello.upload.domain.UploadFile;
import hello.upload.file.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;
    private final FileStore fileStore;

    //등록 폼을 보여준다.
    @GetMapping("/items/new")
    public String newItem(@ModelAttribute ItemForm Form) {
        return "item-form";
    }

    //폼의 데이터를 저장하고 보여주는 화면으로 리다이렉트 한다.
    @PostMapping("/items/new")
    public String saveItem(@ModelAttribute ItemForm form, RedirectAttributes redirectAttributes) throws IOException {
        //MultipartFile attachFile = form.getAttachFile();
        //사용자가 올린 file 이름
        UploadFile attachFile = fileStore.storeFile(form.getAttachFile());

        //List<MultipartFile> imageFiles = form.getImageFiles();
        //서버에 저장 될 파일 이름
        List<UploadFile> storeImageFiles = fileStore.storeFiles(form.getImageFiles());

        //데이터베이스 저장
        Item item = new Item();
        item.setItemName(form.getItemName());
        item.setAttachFile(attachFile);
        item.setImageFiles(storeImageFiles);
        itemRepository.save(item);
        //데이터 베이스에 저장 후 화면 redirect
        redirectAttributes.addAttribute("itemId", item.getId());
        return "redirect:/items/{itemId}";
    }

    //상품 화면
    @GetMapping("/items/{id}")
    public String items(@PathVariable Long id, Model model) {
        //PathVariable 해당하는 상품 페이지
        Item item = itemRepository.findById(id);
        model.addAttribute("item", item);
        return "item-view";

    }

    //<img> 태그로 이미지를 조회할 때 사용한다. UrlResource 이미지 파일을 읽은 후
    //@ResponseBody 로 이미지 바이너리를 반환한다.
    @ResponseBody
    @GetMapping("/images/{filename}")
    public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
        //이 경로에 있는 파일에 접근해서 stream으로 반환해준다.
        return new UrlResource("file:" + fileStore.getFullPath(filename));
    }

    //파일을 다운로드 할 때 실행한다.
    //파일 다운로드 시 권한 체크같은 복잡한 상황까지 가정한다 생각하고 이미지 id 를 요청하도록 했다. 파일
    //다운로드시에는 고객이 업로드한 파일 이름으로 다운로드 하는게 좋다. 이때는 Content-Disposition
    //해더에 attachment; filename="업로드 파일명" 값을 주면 된다
    @GetMapping("/attach/{itemId}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long itemId) throws MalformedURLException {
        Item item = itemRepository.findById(itemId);
        //서버에 저장한 이름
        String storeFileName = item.getAttachFile().getStoreFileName();
        //사용자가 upload 할 때 이름
        String uploadFileName = item.getAttachFile().getUploadFileName();

        UrlResource resource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));

        log.info("uploadFileName={}", uploadFileName);

        //파일 내용 encoding
        String encodedUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename=\"" + encodedUploadFileName +"\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);

    }
}
