package com.neu.cloudwebapp.file;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

//    private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
//            .withRegion(Regions.US_EAST_1)
//            .build();

    private AmazonS3 s3;

    @Autowired
    private FileRepository fileRepository;

    public HashMap<String, Object> getFileData(UUID uuid) {
        try {
            HashMap<String, Object> obj = new HashMap<>();

            Optional<File> f = fileRepository.findById(uuid);

            obj.put("file_id", f.get().getFile_id().toString());
            obj.put("s3_object_name", f.get().getS3_object_name());
            obj.put("file_name", f.get().getFileName());
            obj.put("created_date", f.get().getCreated_date());

            return obj;
        }
        catch(Exception ex){
            System.out.println(ex);
            return null;
        }
    }

    @Value("${BUCKET_NAME:#{null}}")
    private String bucket_name;

    public ObjectMetadata saveFileS3(UUID id, MultipartFile file) throws Exception{

        S3Object fullObject = null;

        try {

            s3 = new AmazonS3Client();

            String fileNewName = id + "-" + file.getOriginalFilename();

            ObjectMetadata objectMetadata = new ObjectMetadata();

            objectMetadata.setContentType(file.getContentType());


            PutObjectRequest obj = new PutObjectRequest(bucket_name, fileNewName, file.getInputStream(),
                    objectMetadata).withCannedAcl(CannedAccessControlList.Private);

            s3.putObject(obj);

            fullObject = s3.getObject(new GetObjectRequest(bucket_name, obj.getKey()));


        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
        }

        return fullObject.getObjectMetadata();

    }

    public void deleteFileS3(String s3_object_name) throws Exception{

        try {
            s3 = new AmazonS3Client();
            s3.deleteObject(new DeleteObjectRequest(bucket_name, s3_object_name));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
        }

    }



}
