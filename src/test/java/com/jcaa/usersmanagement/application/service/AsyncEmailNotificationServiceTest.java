package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.config.AsyncNotificationConfig;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@DisplayName("EmailNotificationService asíncrono")
class AsyncEmailNotificationServiceTest {

  @Test
  @DisplayName("La creación de usuario no espera a que termine el envío SMTP")
  void shouldReturnWithoutWaitingForEmailDelivery() throws InterruptedException {
    final EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
    final CountDownLatch sendingStarted = new CountDownLatch(1);
    final CountDownLatch allowSendingToFinish = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              sendingStarted.countDown();
              allowSendingToFinish.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(emailSenderPort)
        .send(any());

    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AsyncNotificationConfig.class, EmailNotificationService.class);
      context.registerBean(EmailSenderPort.class, () -> emailSenderPort);
      context.refresh();
      final EmailNotificationService service = context.getBean(EmailNotificationService.class);

      try {
        assertTimeout(
            Duration.ofSeconds(1),
            () -> service.notifyUserCreated(createUser(), "Secure123!"));
        assertTrue(sendingStarted.await(1, TimeUnit.SECONDS));
      } finally {
        allowSendingToFinish.countDown();
      }
    }
  }

  private static UserModel createUser() {
    return new UserModel(
        new UserId("8f684dc4-0c67-4813-8d58-7b5e7f7937b8"),
        new UserName("Ada Lovelace"),
        new UserEmail("ada@example.com"),
        UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
        UserRole.ADMIN,
        UserStatus.ACTIVE);
  }
}
