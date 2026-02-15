package com.devos.core.service;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

public interface CommandSandboxService {
    
    @Data
    @Builder
    class CommandResult {
        private String output;
        private String error;
        private int exitCode;
        private boolean success;
        private long executionTimeMs;
    }

    CommandResult executeCommand(String command, String workingDirectory, Map<String, String> environment);
    
    boolean isCommandSafe(String command);
}
