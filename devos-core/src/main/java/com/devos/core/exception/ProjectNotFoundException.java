package com.devos.core.exception;

public class ProjectNotFoundException extends DevosException {
    
    public ProjectNotFoundException(Long projectId) {
        super("Project with ID '" + projectId + "' not found");
    }
    
    public ProjectNotFoundException(String projectName) {
        super("Project with name '" + projectName + "' not found");
    }
}
