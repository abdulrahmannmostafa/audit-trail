package com.qeema.util.document.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentVersionResponse;
import com.qeema.util.document.entity.Document;
import com.qeema.util.document.entity.DocumentVersion;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "filename", source = "version.originalFilename")
    @Mapping(target = "contentType", source = "version.contentType")
    @Mapping(target = "sizeBytes", source = "version.sizeBytes")
    @Mapping(target = "id", source = "document.id")
    @Mapping(target = "createdAt", source = "document.createdAt")
    @Mapping(target = "updatedAt", source = "document.updatedAt")
    DocumentResponse toResponse(Document document, DocumentVersion version);

    DocumentVersionResponse toVersionResponse(DocumentVersion version);
}
