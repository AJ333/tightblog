/*
   Copyright 2017 Glen Mazza

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.tightblog.rendering.processors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tightblog.business.JPAPersistenceStrategy;
import org.tightblog.business.MailManager;
import org.tightblog.business.UserManager;
import org.tightblog.business.WeblogEntryManager;
import org.tightblog.business.WeblogManager;
import org.tightblog.business.search.IndexManager;
import org.tightblog.pojos.Weblog;
import org.tightblog.pojos.WeblogEntry;
import org.tightblog.pojos.WeblogEntryComment;
import org.tightblog.pojos.WeblogEntryComment.ApprovalStatus;
import org.tightblog.pojos.WeblogRole;
import org.tightblog.pojos.WebloggerProperties;
import org.tightblog.pojos.WebloggerProperties.CommentPolicy;
import org.tightblog.rendering.cache.CacheManager;
import org.tightblog.rendering.comment.CommentAuthenticator;
import org.tightblog.rendering.comment.CommentValidator;
import org.tightblog.rendering.comment.CommentValidator.ValidationResult;
import org.tightblog.rendering.requests.WeblogPageRequest;
import org.tightblog.util.HTMLSanitizer;
import org.tightblog.util.I18nMessages;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CommentProcessorTest {

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private RequestDispatcher mockRequestDispatcher;
    private JPAPersistenceStrategy mockJPA;
    private WebloggerProperties properties;
    private CommentProcessor processor;
    private WeblogPageRequest.Creator wprCreator;
    private WeblogPageRequest commentRequest;
    private I18nMessages mockMessageUtils;
    private WeblogManager mockWM;
    private WeblogEntryManager mockWEM;

    @Before
    public void initialize() {
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
        mockRequestDispatcher = mock(RequestDispatcher.class);
        when(mockRequest.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);
        mockJPA = mock(JPAPersistenceStrategy.class);
        properties = new WebloggerProperties();
        properties.setCommentHtmlPolicy(HTMLSanitizer.Level.LIMITED);
        when(mockJPA.getWebloggerProperties()).thenReturn(properties);
        wprCreator = mock(WeblogPageRequest.Creator.class);
        commentRequest = new WeblogPageRequest();
        when(wprCreator.create(any())).thenReturn(commentRequest);
        mockWM = mock(WeblogManager.class);
        mockWEM = mock(WeblogEntryManager.class);
        processor = new CommentProcessor();
        processor.setPersistenceStrategy(mockJPA);
        processor.setWeblogPageRequestCreator(wprCreator);
        processor.setWeblogManager(mockWM);
        processor.setWeblogEntryManager(mockWEM);
        mockMessageUtils = mock(I18nMessages.class);
    }

    @Test
    public void postCommentReturn403IfCommentingDisabled() {
        properties.setCommentPolicy(WebloggerProperties.CommentPolicy.NONE);

        try {
            processor.postComment(mockRequest, mockResponse);
            verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException | ServletException e) {
            fail();
        }
    }

    @Test
    public void postCommentReturn404IfWeblogNotFound() {
        try {
            processor.postComment(mockRequest, mockResponse);
            verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException | ServletException e) {
            fail();
        }
    }

    @Test
    public void postCommentReturn404IfWeblogEntryUnavailable() {
        when(mockWM.getWeblogByHandle(any(), eq(true))).thenReturn(new Weblog());

        try {
            // entry null
            processor.postComment(mockRequest, mockResponse);

            // entry available but not published
            WeblogEntry entry = new WeblogEntry();
            entry.setStatus(WeblogEntry.PubStatus.DRAFT);
            when(mockWEM.getWeblogEntryByAnchor(any(), any())).thenReturn(entry);
            processor.postComment(mockRequest, mockResponse);
            verify(mockResponse, times(2)).sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException | ServletException e) {
            fail();
        }
    }

    @Test
    public void testValidationErrorForwarding() {
        processor = Mockito.spy(processor);

        Weblog weblog = new Weblog();
        weblog.setLocale("en");
        weblog.setHandle("myhandle");
        commentRequest.setWeblog(weblog);

        WeblogEntry entry = new WeblogEntry();
        entry.setAnchor("myblogentry");
        entry.setStatus(WeblogEntry.PubStatus.PUBLISHED);

        when(mockWM.getWeblogByHandle(any(), eq(true))).thenReturn(weblog);
        when(mockWEM.getWeblogEntryByAnchor(any(), any())).thenReturn(entry);

        WeblogEntryComment incomingComment = new WeblogEntryComment();
        // doReturn.when vs. when.theReturn wrt spies: https://stackoverflow.com/a/29394497/1207540
        Mockito.doReturn(incomingComment).when(processor).createCommentFromRequest(eq(mockRequest), eq(entry), any());
        Mockito.doReturn(mockMessageUtils).when(processor).getI18nMessages(any(Locale.class));

        try {
            // will return disabled if comments not allowed
            when(mockWEM.canSubmitNewComments(entry)).thenReturn(false);
            verifyForwardDueToValidationError(incomingComment, "comments.disabled", null);

            // comments allowed, but will show auth error if latter failed
            when(mockWEM.canSubmitNewComments(entry)).thenReturn(true);
            incomingComment.setPreview(false);
            when(mockRequest.getParameter("answer")).thenReturn("123");

            CommentAuthenticator mockAuthenticator = mock(CommentAuthenticator.class);
            when(mockAuthenticator.authenticate(mockRequest)).thenReturn(false);
            processor.setCommentAuthenticator(mockAuthenticator);
            verifyForwardDueToValidationError(incomingComment, "error.commentAuthFailed", "123");

            // ensure auth not checked if preview and content remains in commentForm
            incomingComment.setPreview(true);
            incomingComment.setContent("invalid content should remain");
            Mockito.doReturn("error.commentPostNameMissing").when(processor).validateComment(incomingComment);
            verifyForwardDueToValidationError(incomingComment, "error.commentPostNameMissing", null);
            ArgumentCaptor<WeblogEntryComment> commentCaptor = ArgumentCaptor.forClass(WeblogEntryComment.class);
            verify(mockRequest).setAttribute(eq("commentForm"), commentCaptor.capture());
            assertEquals("invalid content should remain", commentCaptor.getValue().getContent());

        } catch (IOException | ServletException e) {
            fail();
        }
    }

    private void verifyForwardDueToValidationError(WeblogEntryComment incomingComment, String errorProperty,
                                                   String errorValue)
            throws ServletException, IOException {
        Mockito.clearInvocations(mockMessageUtils, mockRequest, mockRequestDispatcher);
        processor.postComment(mockRequest, mockResponse);
        assertTrue(incomingComment.isInvalid());
        verify(mockMessageUtils).getString(errorProperty, errorValue);
        verify(mockRequest).setAttribute("commentForm", incomingComment);
        verify(mockRequest).getRequestDispatcher(PageProcessor.PATH + "/myhandle/entry/myblogentry");
        verify(mockRequestDispatcher).forward(mockRequest, mockResponse);
    }

    @Test
    public void testCommentSpamChecking() {
        processor = Mockito.spy(processor);

        // setup to ensure comment is at least valid so spam check can start
        commentRequest.setWeblogHandle("myhandle");

        WeblogEntry entry = new WeblogEntry();
        entry.setAnchor("myblogentry");
        entry.setStatus(WeblogEntry.PubStatus.PUBLISHED);

        CommentAuthenticator mockAuthenticator = mock(CommentAuthenticator.class);
        when(mockAuthenticator.authenticate(mockRequest)).thenReturn(true);
        processor.setCommentAuthenticator(mockAuthenticator);

        Weblog weblog = new Weblog();
        weblog.setLocale("en");
        weblog.setHandle("myhandle");
        when(mockWM.getWeblogByHandle("myhandle", true)).thenReturn(weblog);
        when(mockWEM.getWeblogEntryByAnchor(any(), any())).thenReturn(entry);
        when(mockWEM.canSubmitNewComments(entry)).thenReturn(true);

        UserManager mockUM = mock(UserManager.class);
        processor.setUserManager(mockUM);

        MailManager mockMM = mock(MailManager.class);
        processor.setMailManager(mockMM);

        CacheManager mockCM = mock(CacheManager.class);
        processor.setCacheManager(mockCM);

        IndexManager mockIM = mock(IndexManager.class);
        when(mockIM.isIndexComments()).thenReturn(true);
        processor.setIndexManager(mockIM);

        WeblogEntryComment incomingComment = new WeblogEntryComment();
        incomingComment.setPreview(false);
        Mockito.doReturn(incomingComment).when(processor).createCommentFromRequest(eq(mockRequest), eq(entry), any());
        Mockito.doReturn(mockMessageUtils).when(processor).getI18nMessages(any(Locale.class));
        Mockito.doReturn(null).when(processor).validateComment(incomingComment);

        try {
            // test that if it is the blogger's comment it is automatically not spam
            // make this a blogger's comment
            when(mockUM.checkWeblogRole(any(), anyString(), eq(WeblogRole.POST))).thenReturn(true);
            // have it evaluate to spam
            Mockito.doReturn(ValidationResult.SPAM).when(processor).runSpamCheckers(eq(incomingComment), any());
            // still approved
            incomingComment.setContent("valid content should get cleared");
            verifyForwardAfterSpamChecking(incomingComment, ApprovalStatus.APPROVED, "commentServlet.commentAccepted");
            ArgumentCaptor<WeblogEntryComment> commentCaptor = ArgumentCaptor.forClass(WeblogEntryComment.class);
            verify(mockRequest).setAttribute(eq("commentForm"), commentCaptor.capture());
            assertEquals("", commentCaptor.getValue().getContent());

            // make subsequent tests a non-blogger comment
            when(mockUM.checkWeblogRole(any(), anyString(), eq(WeblogRole.POST))).thenReturn(false);

            // check spam comment requires approval even if must moderate off
            properties.setCommentPolicy(CommentPolicy.YES);
            weblog.setAllowComments(CommentPolicy.YES);
            verifyForwardAfterSpamChecking(incomingComment, ApprovalStatus.SPAM, "commentServlet.submittedToModerator");

            Mockito.clearInvocations(mockWEM, mockMM, mockIM, mockCM);

            // check non-spam comment doesn't require approval if must moderate off
            Mockito.doReturn(ValidationResult.NOT_SPAM).when(processor).runSpamCheckers(eq(incomingComment), any());
            properties.setCommentPolicy(CommentPolicy.YES);
            weblog.setAllowComments(CommentPolicy.YES);
            verifyForwardAfterSpamChecking(incomingComment, ApprovalStatus.APPROVED, "commentServlet.commentAccepted");
            verify(mockMM).sendNewPublishedCommentNotification(incomingComment);
            verify(mockIM).updateIndex(entry, false);
            verify(mockCM).invalidate(incomingComment);

            // check non-spam comment requires approval if must moderate set globally
            properties.setCommentPolicy(CommentPolicy.MUSTMODERATE);
            verifyForwardAfterSpamChecking(incomingComment, ApprovalStatus.PENDING, "commentServlet.submittedToModerator");

            // check non-spam comment requires approval if must moderate set for blog
            properties.setCommentPolicy(CommentPolicy.YES);
            weblog.setAllowComments(CommentPolicy.MUSTMODERATE);
            verifyForwardAfterSpamChecking(incomingComment, ApprovalStatus.PENDING, "commentServlet.submittedToModerator");

            // check no indexing if indexing shut off
            Mockito.clearInvocations(mockWEM, mockMM, mockIM, mockCM);
            weblog.setAllowComments(CommentPolicy.YES);
            when(mockIM.isIndexComments()).thenReturn(false);
            processor.postComment(mockRequest, mockResponse);
            verify(mockMM).sendNewPublishedCommentNotification(incomingComment);
            verify(mockIM, never()).updateIndex(entry, false);

            // check spam persisted to database with autodelete spam off
            Mockito.clearInvocations(mockWEM, mockMM, mockIM, mockCM);
            Mockito.doReturn(ValidationResult.SPAM).when(processor).runSpamCheckers(eq(incomingComment), any());
            properties.setAutodeleteSpam(false);
            processor.postComment(mockRequest, mockResponse);
            verify(mockWEM).saveComment(eq(incomingComment), anyBoolean());
            verify(mockMM).sendPendingCommentNotice(eq(incomingComment), any());
            verify(mockCM, never()).invalidate(incomingComment);
            verify(mockIM, never()).updateIndex(any(WeblogEntry.class), anyBoolean());

            // check spam not persisted to database with autodelete spam on
            Mockito.clearInvocations(mockWEM);
            properties.setAutodeleteSpam(true);
            processor.postComment(mockRequest, mockResponse);
            verify(mockWEM, never()).saveComment(eq(incomingComment), anyBoolean());

            Mockito.clearInvocations(mockWEM);

            // check blatant spam not persisted to database
            Mockito.doReturn(ValidationResult.BLATANT_SPAM).when(processor).runSpamCheckers(eq(incomingComment), any());
            processor.postComment(mockRequest, mockResponse);
            verify(mockWEM, never()).saveComment(eq(incomingComment), anyBoolean());

        } catch (IOException | ServletException e) {
            fail();
        }
    }

    private void verifyForwardAfterSpamChecking(WeblogEntryComment incomingComment, ApprovalStatus status,
                                                   String commentStatusKey)
            throws ServletException, IOException {
        Mockito.clearInvocations(mockMessageUtils, mockRequestDispatcher);
        processor.postComment(mockRequest, mockResponse);
        assertEquals(status, incomingComment.getStatus());
        verify(mockMessageUtils).getString(commentStatusKey);
        verify(mockRequestDispatcher).forward(mockRequest, mockResponse);
    }

    @Test
    public void testCreateCommentFromRequest() {
        when(mockRequest.getParameter("notify")).thenReturn("anything");
        when(mockRequest.getParameter("email")).thenReturn("bob@email.com");
        when(mockRequest.getParameter("url")).thenReturn("www.foo.com");
        when(mockRequest.getParameter("content")).thenReturn("Enjoy <a href=\"http://www.abc.com\">My Link</a> from Bob!");
        when(mockRequest.getParameter("name")).thenReturn("Bob");
        when(mockRequest.getParameter("preview")).thenReturn(null);
        when(mockRequest.getRemoteHost()).thenReturn("http://www.bar.com");

        WeblogEntry entry = new WeblogEntry();

        WeblogEntryComment wec = processor.createCommentFromRequest(mockRequest, entry, HTMLSanitizer.Level.LIMITED);

        assertEquals("Content not processed correctly (text, whitelist filtering of tags, and adding paragraph tags)",
                "<p>Enjoy My Link from Bob!</p>", wec.getContent());
        assertEquals("Bob", wec.getName());
        assertFalse("Bob", wec.isPreview());
        assertEquals("http:// not added to URL", "http://www.foo.com", wec.getUrl());
        assertTrue(wec.getNotify());
        assertEquals("bob@email.com", wec.getEmail());
        assertEquals("http://www.bar.com", wec.getRemoteHost());
        assertEquals(entry, wec.getWeblogEntry());
        assertNotNull(wec.getPostTime());

        when(mockRequest.getParameter("notify")).thenReturn(null);
        when(mockRequest.getParameter("preview")).thenReturn("true");
        wec = processor.createCommentFromRequest(mockRequest, entry, HTMLSanitizer.Level.BASIC);
        assertFalse(wec.getNotify());
        assertTrue(wec.isPreview());
        assertEquals("Content not processed correctly (text and whitelist filtering of tags)",
                "<p>Enjoy <a href=\"http://www.abc.com\" rel=\"nofollow\">My Link</a> from Bob!</p>", wec.getContent());

        when(mockRequest.getParameter("preview")).thenReturn("false");
        wec = processor.createCommentFromRequest(mockRequest, entry, HTMLSanitizer.Level.BASIC);
        assertFalse(wec.isPreview());
    }

    @Test
    public void testValidateComment() {
        WeblogEntryComment comment = new WeblogEntryComment();

        comment.setContent("Hi!");
        comment.setName("Bob");
        comment.setEmail("bob@email.com");
        comment.setUrl("http://www.bob.com");

        String response = processor.validateComment(comment);
        assertTrue(response == null);

        comment.setUrl("ftp://www.myftpsite.com");
        response = processor.validateComment(comment);
        assertEquals("error.commentPostFailedURL", response);
        comment.setUrl("http://www.bob.com");

        comment.setEmail("bob@abc");
        response = processor.validateComment(comment);
        assertEquals("error.commentPostFailedEmailAddress", response);
        comment.setEmail("");
        assertEquals("error.commentPostFailedEmailAddress", response);
        comment.setEmail("bob@email.com");

        comment.setName(null);
        response = processor.validateComment(comment);
        assertEquals("error.commentPostNameMissing", response);
        comment.setName("Bob");

        comment.setContent("  ");
        response = processor.validateComment(comment);
        assertEquals("error.commentPostContentMissing", response);
    }

    @Test
    public void testGenerateAuthForm() throws IOException {
        String authHTML = "difficult math question";
        CommentAuthenticator mockAuthenticator = mock(CommentAuthenticator.class);
        when(mockAuthenticator.getHtml(any(HttpServletRequest.class))).thenReturn(authHTML);

        PrintWriter mockWriter = mock(PrintWriter.class);
        when(mockResponse.getWriter()).thenReturn(mockWriter);

        processor.setCommentAuthenticator(mockAuthenticator);
        processor.generateAuthForm(mockRequest, mockResponse);

        verify(mockResponse).setContentType("text/html; charset=utf-8");
        verify(mockResponse).addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(mockResponse).addHeader("Pragma", "no-cache");
        verify(mockResponse).addHeader("Expires", "-1");
        verify(mockWriter).println(authHTML);
    }

    @Test
    public void testValidateCommentViaValidators() {
        WeblogEntryComment testComment = new WeblogEntryComment();
        Map<String, List<String>> testMessages = new HashMap<String, List<String>>();

        CommentValidator mockAlwaysBlatantValidator = mock(CommentValidator.class);
        when(mockAlwaysBlatantValidator.validate(any(WeblogEntryComment.class), anyMap()))
                .thenReturn(CommentValidator.ValidationResult.BLATANT_SPAM);

        CommentValidator mockAlwaysSpamValidator = mock(CommentValidator.class);
        when(mockAlwaysSpamValidator.validate(any(WeblogEntryComment.class), anyMap()))
                .thenReturn(CommentValidator.ValidationResult.SPAM);

        CommentValidator mockAlwaysNonSpamValidator = mock(CommentValidator.class);
        when(mockAlwaysNonSpamValidator.validate(any(WeblogEntryComment.class), anyMap()))
                .thenReturn(CommentValidator.ValidationResult.NOT_SPAM);

        List<CommentValidator> emptyList = new ArrayList<>();
        List<CommentValidator> oneBlatantOneNonSpam = new ArrayList<>();
        oneBlatantOneNonSpam.add(mockAlwaysBlatantValidator);
        oneBlatantOneNonSpam.add(mockAlwaysNonSpamValidator);
        List<CommentValidator> oneSpamOneNonSpam = new ArrayList<>();
        oneSpamOneNonSpam.add(mockAlwaysSpamValidator);
        oneSpamOneNonSpam.add(mockAlwaysNonSpamValidator);
        List<CommentValidator> twoNonSpam = new ArrayList<>();
        twoNonSpam.add(mockAlwaysNonSpamValidator);
        twoNonSpam.add(mockAlwaysNonSpamValidator);

        processor.setCommentValidators(emptyList);
        assertEquals(CommentValidator.ValidationResult.NOT_SPAM, processor.runSpamCheckers(testComment, testMessages));

        processor.setCommentValidators(oneBlatantOneNonSpam);
        assertEquals(CommentValidator.ValidationResult.BLATANT_SPAM, processor.runSpamCheckers(testComment, testMessages));

        processor.setCommentValidators(oneSpamOneNonSpam);
        assertEquals(CommentValidator.ValidationResult.SPAM, processor.runSpamCheckers(testComment, testMessages));
        oneSpamOneNonSpam.add(mockAlwaysBlatantValidator);
        assertEquals(CommentValidator.ValidationResult.BLATANT_SPAM, processor.runSpamCheckers(testComment, testMessages));

        processor.setCommentValidators(twoNonSpam);
        assertEquals(CommentValidator.ValidationResult.NOT_SPAM, processor.runSpamCheckers(testComment, testMessages));
        twoNonSpam.add(mockAlwaysSpamValidator);
        assertEquals(CommentValidator.ValidationResult.SPAM, processor.runSpamCheckers(testComment, testMessages));
    }
}