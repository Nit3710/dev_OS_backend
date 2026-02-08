package com.devos.file.service.impl;

import com.devos.file.service.GitService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GitServiceImpl implements GitService {

    @Override
    public String getCurrentCommitHash(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            ObjectId head = git.getRepository().resolve("HEAD");
            return head != null ? head.getName() : null;
        } catch (Exception e) {
            log.error("Error getting current commit hash", e);
            return null;
        }
    }

    @Override
    public String createCommit(String projectPath, String message, String authorName, String authorEmail) {
        try (Git git = openGitRepository(projectPath)) {
            // Add all changes
            git.add().addFilepattern(".").call();
            
            // Create commit
            RevCommit commit = git.commit()
                    .setMessage(message)
                    .setAuthor(authorName, authorEmail)
                    .call();
            
            log.info("Created commit: {} in project: {}", commit.getId(), projectPath);
            return commit.getId().getName();
        } catch (GitAPIException e) {
            log.error("Error creating commit", e);
            throw new RuntimeException("Failed to create commit", e);
        }
    }

    @Override
    public List<Map<String, Object>> getCommitHistory(String projectPath, int limit) {
        try (Git git = openGitRepository(projectPath)) {
            Iterable<RevCommit> commits = git.log().setMaxCount(limit).call();
            
            List<Map<String, Object>> commitList = new ArrayList<>();
            for (RevCommit commit : commits) {
                Map<String, Object> commitMap = new HashMap<>();
                commitMap.put("hash", commit.getId().getName());
                commitMap.put("shortHash", commit.getId().abbreviate(8).name());
                commitMap.put("message", commit.getFullMessage());
                commitMap.put("author", commit.getAuthorIdent().getName());
                commitMap.put("email", commit.getAuthorIdent().getEmailAddress());
                commitMap.put("timestamp", commit.getCommitTime() * 1000L);
                commitList.add(commitMap);
            }
            
            return commitList;
        } catch (GitAPIException e) {
            log.error("Error getting commit history", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String createBranch(String projectPath, String branchName) {
        try (Git git = openGitRepository(projectPath)) {
            git.branchCreate().setName(branchName).call();
            log.info("Created branch: {} in project: {}", branchName, projectPath);
            return branchName;
        } catch (GitAPIException e) {
            log.error("Error creating branch", e);
            throw new RuntimeException("Failed to create branch", e);
        }
    }

    @Override
    public void checkoutBranch(String projectPath, String branchName) {
        try (Git git = openGitRepository(projectPath)) {
            git.checkout().setName(branchName).call();
            log.info("Checked out branch: {} in project: {}", branchName, projectPath);
        } catch (GitAPIException e) {
            log.error("Error checking out branch", e);
            throw new RuntimeException("Failed to checkout branch", e);
        }
    }

    @Override
    public List<String> getBranches(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            List<Ref> branches = git.branchList().call();
            return branches.stream()
                    .map(ref -> ref.getName().replace("refs/heads/", ""))
                    .collect(Collectors.toList());
        } catch (GitAPIException e) {
            log.error("Error getting branches", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String mergeBranch(String projectPath, String sourceBranch, String targetBranch) {
        try (Git git = openGitRepository(projectPath)) {
            // Checkout target branch
            git.checkout().setName(targetBranch).call();
            
            // Merge source branch
            git.merge().include(git.getRepository().findRef(sourceBranch)).call();
            
            log.info("Merged branch {} into {} in project: {}", sourceBranch, targetBranch, projectPath);
            return getCurrentCommitHash(projectPath);
        } catch (Exception e) {
            log.error("Error merging branches", e);
            throw new RuntimeException("Failed to merge branches", e);
        }
    }

    @Override
    public void revertCommit(String projectPath, String commitHash) {
        try (Git git = openGitRepository(projectPath)) {
            git.revert().setOurCommitName(commitHash).call();
            log.info("Reverted commit: {} in project: {}", commitHash, projectPath);
        } catch (GitAPIException e) {
            log.error("Error reverting commit", e);
            throw new RuntimeException("Failed to revert commit", e);
        }
    }

    @Override
    public Map<String, Object> getFileStatus(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            Map<String, Object> status = new HashMap<>();
            
            var statusCommand = git.status().call();
            status.put("modified", statusCommand.getModified());
            status.put("added", statusCommand.getAdded());
            status.put("removed", statusCommand.getRemoved());
            status.put("untracked", statusCommand.getUntracked());
            status.put("conflicting", statusCommand.getConflicting());
            status.put("clean", statusCommand.isClean());
            
            return status;
        } catch (GitAPIException e) {
            log.error("Error getting file status", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<String> getChangedFiles(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            var status = git.status().call();
            List<String> changedFiles = new ArrayList<>();
            changedFiles.addAll(status.getModified());
            changedFiles.addAll(status.getAdded());
            changedFiles.addAll(status.getRemoved());
            changedFiles.addAll(status.getUntracked());
            return changedFiles;
        } catch (GitAPIException e) {
            log.error("Error getting changed files", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String stashChanges(String projectPath, String message) {
        try (Git git = openGitRepository(projectPath)) {
            RevCommit stashCommit = git.stashCreate().setWorkingDirectoryMessage(message).call();
            log.info("Stashed changes in project: {}", projectPath);
            return stashCommit != null ? stashCommit.getId().getName() : null;
        } catch (GitAPIException e) {
            log.error("Error stashing changes", e);
            throw new RuntimeException("Failed to stash changes", e);
        }
    }

    @Override
    public List<String> getStashList(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            var stashList = git.stashList().call();
            return stashList.stream()
                    .map(stash -> stash.getId().getName())
                    .collect(Collectors.toList());
        } catch (GitAPIException e) {
            log.error("Error getting stash list", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void applyStash(String projectPath, String stashId) {
        try (Git git = openGitRepository(projectPath)) {
            git.stashApply().setStashRef(stashId).call();
            log.info("Applied stash: {} in project: {}", stashId, projectPath);
        } catch (GitAPIException e) {
            log.error("Error applying stash", e);
            throw new RuntimeException("Failed to apply stash", e);
        }
    }

    @Override
    public void discardChanges(String projectPath, String filePath) {
        try (Git git = openGitRepository(projectPath)) {
            git.checkout().addPath(filePath).call();
            log.info("Discarded changes for file: {} in project: {}", filePath, projectPath);
        } catch (GitAPIException e) {
            log.error("Error discarding changes", e);
            throw new RuntimeException("Failed to discard changes", e);
        }
    }

    @Override
    public Map<String, Object> getRemoteStatus(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            Map<String, Object> remoteStatus = new HashMap<>();
            
            // Get remote branches
            List<Ref> remoteBranches = git.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call();
            remoteStatus.put("remoteBranches", remoteBranches.stream()
                    .map(ref -> ref.getName().replace("refs/remotes/", ""))
                    .collect(Collectors.toList()));
            
            // Get ahead/behind info
            try (RevWalk walk = new RevWalk(git.getRepository())) {
                ObjectId head = git.getRepository().resolve("HEAD");
                if (head != null) {
                    RevCommit headCommit = walk.parseCommit(head);
                    // This is simplified - in production you'd want more detailed tracking
                    remoteStatus.put("currentBranch", git.getRepository().getBranch());
                    remoteStatus.put("headCommit", headCommit.getId().getName());
                }
            }
            
            return remoteStatus;
        } catch (Exception e) {
            log.error("Error getting remote status", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void pullChanges(String projectPath) {
        try (Git git = openGitRepository(projectPath)) {
            git.pull().call();
            log.info("Pulled changes in project: {}", projectPath);
        } catch (GitAPIException e) {
            log.error("Error pulling changes", e);
            throw new RuntimeException("Failed to pull changes", e);
        }
    }

    @Override
    public void pushChanges(String projectPath, String branchName) {
        try (Git git = openGitRepository(projectPath)) {
            git.push().add(branchName).call();
            log.info("Pushed changes for branch: {} in project: {}", branchName, projectPath);
        } catch (GitAPIException e) {
            log.error("Error pushing changes", e);
            throw new RuntimeException("Failed to push changes", e);
        }
    }

    private Git openGitRepository(String projectPath) {
        try {
            File gitDir = new File(projectPath, ".git");
            if (!gitDir.exists()) {
                // Initialize git repository if it doesn't exist
                Git.init().setDirectory(new File(projectPath)).call();
            }
            
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();
            
            return new Git(repository);
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to open git repository", e);
        }
    }
}
