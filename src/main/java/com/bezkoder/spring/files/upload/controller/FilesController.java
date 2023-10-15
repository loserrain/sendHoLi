package com.bezkoder.spring.files.upload.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.bezkoder.spring.files.upload.service.QRCodeGenerator;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.bezkoder.spring.files.upload.message.ResponseMessage;
import com.bezkoder.spring.files.upload.model.FileInfo;
import com.bezkoder.spring.files.upload.service.FilesStorageService;

@Controller
@CrossOrigin(origins = {"http://localhost:8081","http://127.0.0.1:8081","http://localhost:5173"})
public class FilesController {

  @Autowired
  FilesStorageService storageService;

  //   @RequestParam("file") 註解表示從HTTP請求中獲取名為“file”的檔案參數，
//   並將其綁定到MultipartFile類型的file參數上。
  @PostMapping("/upload")
  public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
    String message = "";
    try {
      storageService.save(file);

      message = "Uploaded the file successfully: " + file.getOriginalFilename();



      return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
    } catch (Exception e) {
      message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
    }
  }


  @GetMapping("/files")
  public ResponseEntity<List<FileInfo>> getListFiles() {
//  storageService.loadAll()是從儲存服務中檢索所有檔案路徑。
//  然後對這些路徑中的每一個都應用.map()函數。在map函數內部：
//  使用path.getFileName().toString()從路徑中提取檔案名稱。
//  使用MvcUriComponentsBuilder.fromMethodName()創建檔案的URL。此URL是根據FilesController類中名為"getFile"的方法建立的，並且包含檔案名稱作為參數。
//  每個檔案名稱和URL都用於創建一個新的FileInfo物件。
//  最後，.collect(Collectors.toList())將這些FileInfo物件收集到一個列表中


    //MvcUriComponentsBuilder.fromMethodName() 是 Spring Boot 中的一個方法，用於根據控制器方法名和方法參數來生成 URI。
    /*將 Stream 中的每個檔案路徑轉換為一個 FileInfo 物件。map() 方法會接受一個 Lambda 表達式作為參數，該
    Lambda 表達式會對 Stream 中的每個元素進行操作。在本例中，我們使用 Lambda 表達式來將 filename 和 url 轉換為一個 FileInfo 物件*/
    List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
      String filename = path.getFileName().toString();
      String url = MvcUriComponentsBuilder
              .fromMethodName(FilesController.class, "getFile", path.getFileName().toString()).build().toString();

      return new FileInfo(filename, url);
    }).collect(Collectors.toList());




    return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
  }


  @GetMapping("/files/{filename:.+}")
  public ResponseEntity<Resource> getFile(@PathVariable String filename) {
    Resource file = storageService.load(filename);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
  }


  //{filename:.+}是一個路徑變數，表示檔案名稱，.+表示檔案名稱可以包含點號（.）和擴展名。
//   客戶端發送HTTP DELETE請求到/files/{filename:.+}路徑時，
//   Spring MVC將調用與@DeleteMapping("/files/{filename:.+}")
//   註解相對應的處理程序方法，執行刪除指定檔案的操作。
  @DeleteMapping("/files/{filename:.+}")
  public ResponseEntity<ResponseMessage> deleteFile(@PathVariable String filename) {
    String message = "";

    try {
      boolean existed = storageService.delete(filename);

      if (existed) {
        message = "Delete the file successfully: " + filename;
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
      }

      message = "The file does not exist!";
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseMessage(message));
    } catch (Exception e) {
      message = "Could not delete the file: " + filename + ". Error: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseMessage(message));
    }
  }


//  @GetMapping("/qrcode")
//  public ResponseEntity<ResponseMessage> qrcode() throws MalformedURLException {
////    String medium="https://i.imgur.com/mkLO6dy.mp4";
////    String github="https://github.com/rahul26021999";
//    List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
//      String filename = path.getFileName().toString();
//      String url = MvcUriComponentsBuilder
//              .fromMethodName(FilesController.class, "getFile", path.getFileName().toString()).build().toString();
//
//      return new FileInfo(filename, url);
//    }).toList();
//
//    String lastUrl = fileInfos.get(0).getUrl();
//
//    byte[] image = new byte[0];
//    try {
//
//      // Generate and Return Qr Code in Byte Array
//      image = QRCodeGenerator.getQRCodeImage(lastUrl,250,250);
//
//      // Generate and Save Qr Code Image in static/image folder
//      //QRCodeGenerator.generateQRCodeImage(github,250,250,QR_CODE_IMAGE_PATH);
//
//    } catch (WriterException | IOException e) {
//      e.printStackTrace();
//    }
//    // Convert Byte Array into Base64 Encode String
//    String qrcode = Base64.getEncoder().encodeToString(image);
//
//
//    return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(qrcode));
//
//  }

  @GetMapping("/qrcode")
  public ResponseEntity<ResponseMessage> qrcode() throws MalformedURLException {
    String lastUrl = "";
    try {

      // Load the last file from storage
      lastUrl = storageService.loadLastFile().toUri().toString();

      // Generate and Return Qr Code in Byte Array
      byte[] image = QRCodeGenerator.getQRCodeImage(lastUrl, 250, 250);

      // Convert Byte Array into Base64 Encode String
      String qrcode = Base64.getEncoder().encodeToString(image);

      return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(qrcode));
    } catch (WriterException | IOException e) {
      e.printStackTrace();
      String message = "Could not generate QR code for the file: " + lastUrl + ". Error: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseMessage(message));
    } catch (Exception e) {
      e.printStackTrace();
      String message = "Could not load the last file from storage. Error: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseMessage(message));
    }
  }

}

