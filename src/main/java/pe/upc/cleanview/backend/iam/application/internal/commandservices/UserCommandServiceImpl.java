package pe.upc.cleanview.backend.iam.application.internal.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.upc.cleanview.backend.iam.domain.model.aggregates.User;
import pe.upc.cleanview.backend.iam.domain.model.commands.SignInCommand;
import pe.upc.cleanview.backend.iam.domain.model.commands.SignUpCommand;
import pe.upc.cleanview.backend.iam.domain.services.UserCommandService;
import pe.upc.cleanview.backend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import pe.upc.cleanview.backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import pe.upc.cleanview.backend.iam.application.internal.outboundservices.tokens.TokenService;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder; // ✅ Usamos el encoder de Spring
  private final TokenService tokenService;
  private final RoleRepository roleRepository;

  public UserCommandServiceImpl(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                TokenService tokenService,
                                RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.roleRepository = roleRepository;
  }

  /**
   * Handle the sign-in command
   * <p>
   * This method handles the {@link SignInCommand} command and returns the user and the token.
   * </p>
   * @param command the sign-in command containing the email and password
   * @return an optional containing the user and the generated token
   * @throws RuntimeException if the user is not found or the password is invalid
   */
  @Override
  public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
    var user = userRepository.findByEmail(command.email());
    if (user.isEmpty()) throw new RuntimeException("User not found");

    // ✅ Compara usando BCryptPasswordEncoder
    if (!passwordEncoder.matches(command.password(), user.get().getPassword()))
      throw new RuntimeException("Invalid password");

    var token = tokenService.generateToken(user.get().getUsername());
    return Optional.of(ImmutablePair.of(user.get(), token));
  }

  @Override
  public Optional<User> handle(SignUpCommand command) {
    if (userRepository.existsByUsername(command.username()))
      throw new RuntimeException("Username already exists");
    if (userRepository.existsByEmail(command.email()))
      throw new RuntimeException("Email already exists");

    var user = new User(
            command.username(),
            command.email(),
            passwordEncoder.encode(command.password()), // ✅ Codifica con BCrypt
            command.roles()
    );
    userRepository.save(user);
    return userRepository.findByUsername(command.username());
  }
}
