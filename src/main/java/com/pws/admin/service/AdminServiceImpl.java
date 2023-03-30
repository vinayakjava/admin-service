package com.pws.admin.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pws.admin.dto.PermissionDTO;
import com.pws.admin.dto.SignUpDTO;
import com.pws.admin.dto.UpdatePasswordDTO;
import com.pws.admin.dto.UserBasicDetailsDTO;
import com.pws.admin.dto.UserRoleXrefDTO;
import com.pws.admin.entity.Module;
import com.pws.admin.entity.Permission;
import com.pws.admin.entity.Role;
import com.pws.admin.entity.Skill;
import com.pws.admin.entity.User;
import com.pws.admin.entity.UserRoleXref;
import com.pws.admin.exception.config.PWSException;
import com.pws.admin.repository.ModuleRepository;
import com.pws.admin.repository.PermissionRepository;
import com.pws.admin.repository.RoleRepository;
import com.pws.admin.repository.SkillRepository;
import com.pws.admin.repository.UserRepository;
import com.pws.admin.repository.UserRoleXrefRepository;
import com.pws.admin.utility.DateUtils;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @Author Vinayak M
 * @Date 09/01/23
 */

@Service
public class AdminServiceImpl implements AdminService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	private UserRoleXrefRepository userRoleXrefRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private SkillRepository skillRepository;

	@Autowired
	private JavaMailSender mailSender;

	private Map<String, String> otpMap = new HashMap<>(); // in-memory map to store OTPs

	@Override
	public void sendOTP(String email) throws PWSException, MessagingException, UnsupportedEncodingException {


		// Generate a 6-digit OTP
		int otp = new Random().nextInt(900000) + 100000;

//		// Send the OTP via email
//		final String username = "kiranrajpws@gmail.com"; // Replace with your email address
//		final String password = "Y1SZMBtIANK3kv8D"; // Replace with your email password
//
//		Properties props = new Properties();
//		props.put("mail.smtp.auth", "true");
//		props.put("mail.smtp.starttls.enable", "true");
//		props.put("mail.smtp.host", "smtp-relay.sendinblue.com"); // Replace with your email provider's SMTP server
//		props.put("mail.smtp.port", "587"); // Replace with your email provider's SMTP port
//
//		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
//			protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
//				return new javax.mail.PasswordAuthentication(username, password);



		Message message = mailSender.createMimeMessage();;
		MimeMessageHelper helper = new MimeMessageHelper((MimeMessage) message);
		helper.setFrom("contact@adminpws.com", "Admin Service Support");
		helper.setTo(email);
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
		message.setSubject("OTP Verification");
		message.setText("Your OTP for registration is: " + otp);

		mailSender.send((MimeMessage) message);

		// Store the OTP in the in-memory map for validation
		otpMap.put(email, String.valueOf(otp));
	}

	public boolean verifyOTP(String email, String otp) {
		String storedOTP = otpMap.get(email);
		if (storedOTP == null) {
			return false;
		}
		return storedOTP.equals(otp);
	}



	@Override
	public void UserSignUp(SignUpDTO signupDTO) throws PWSException {
		// Check if the password is strong
		if (!isStrongPassword(signupDTO.getPassword())) {
			throw new PWSException("Password is not strong , at least one uppercase letter, one lowercase letter, one digit, and one special character needed");
		}

		Optional<User> optionalUser = userRepository.findUserByEmail(signupDTO.getEmail());
		if (optionalUser.isPresent()) {
			throw new PWSException("User Already Exist with Email : " + signupDTO.getEmail());
		}

		// Verify the OTP
		String otp = otpMap.get(signupDTO.getEmail());
		if (!otp.equals(signupDTO.getOtp())) {
			throw new PWSException("Invalid OTP");
		}

		User user = new User();
		user.setDateOfBirth(signupDTO.getDateOfBirth());
		user.setFirstName(signupDTO.getFirstName());
		user.setIsActive(true);
		user.setLastName(signupDTO.getLastName());
		user.setEmail(signupDTO.getEmail());
		user.setPhoneNumber(signupDTO.getPhoneNumber());
		PasswordEncoder encoder = new BCryptPasswordEncoder(8);
		// Set new password
		user.setPassword(encoder.encode(signupDTO.getPassword()));

		userRepository.save(user);
	}









	private boolean isStrongPassword(String password) {
		boolean hasUppercase = false;
		boolean hasLowercase = false;
		boolean hasDigit = false;
		boolean hasSpecialChar = false;

		// check for at least one uppercase letter, one lowercase letter, one digit, and one special character
		for (int i = 0; i < password.length(); i++) {
			char ch = password.charAt(i);
			if (Character.isUpperCase(ch)) {
				hasUppercase = true;
			} else if (Character.isLowerCase(ch)) {
				hasLowercase = true;
			} else if (Character.isDigit(ch)) {
				hasDigit = true;
			} else if (isSpecialChar(ch)) {
				hasSpecialChar = true;
			}
		}

		// check if password meets all criteria
		return password.length() >= 8 && hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
	}

	private boolean isSpecialChar(char ch) {
		String specialChars = "!@#$%^&*()_-+=[{]};:<>|./?";
		return specialChars.contains(Character.toString(ch));
	}


	@Override
	public void updateUserPassword(UpdatePasswordDTO userPasswordDTO) throws PWSException {
		Optional<User> optionalUser = userRepository.findUserByEmail(userPasswordDTO.getUserEmail());

		PasswordEncoder encoder = new BCryptPasswordEncoder();
		User user = null;
		if (!optionalUser.isPresent()) {
			throw new PWSException("User Not present ");
		}
		user = optionalUser.get();
		if (encoder.matches(userPasswordDTO.getOldPassword(), user.getPassword())) {
			// Check if the new password is strong
			if (!isStrongPassword(userPasswordDTO.getNewPassword())) {
				throw new PWSException("Password is not strong , at least one uppercase letter, one lowercase letter, one digit, and one special character needed");
			}
			if (encoder.matches(userPasswordDTO.getOldPassword(), user.getPassword())) {
				if (userPasswordDTO.getNewPassword().equals(userPasswordDTO.getConfirmNewPassword())) {
					if (!userPasswordDTO.getOldPassword().equals(userPasswordDTO.getNewPassword())) {
						user.setPassword(encoder.encode(userPasswordDTO.getConfirmNewPassword()));
						userRepository.save(user);
					} else {
						throw new PWSException("new and old password are same");
					}
				} else {
					throw new PWSException("new password and confirm password doesnot match ");
				}

			} else {
				throw new PWSException("old password not matched");
			}
		}
	}




	@Override
	public void updateResetPasswordToken(Integer otp, String email, LocalDateTime expirationTime) throws PWSException {
		User user = userRepository.findByEmail(email);
		if (user != null) {
			user.setResetPasswordOtp(otp);
			user.setResetPasswordExpiry(expirationTime);
			userRepository.save(user);
		} else {
			throw new PWSException("Could not find any customer with the email " + email);
		}
	}


	public User getByResetPasswordToken(Integer otp) {
		return userRepository.findByResetPasswordOtp(otp);
	}

	public void updatePassword(Integer otp, String newPassword, String confirmPassword, LocalDateTime currentTime) throws PWSException {
		// Check if the new password is strong
		if (!isStrongPassword(newPassword)) {
			throw new PWSException("Password is not strong, at least one uppercase letter, one lowercase letter, one digit, and one special character needed");
		}

		PasswordEncoder encoder = new BCryptPasswordEncoder();

		User user = userRepository.findByResetPasswordOtp(otp);
		if (user == null) {
			throw new PWSException("Invalid reset password OTP");
		}

		LocalDateTime expirationTime = user.getResetPasswordExpiry();
		if (expirationTime.isBefore(currentTime)) {
			throw new PWSException("Reset password OTP has expired");
		}

		String oldPassword = user.getPassword();

		if (encoder.matches(newPassword, oldPassword)) {
			throw new PWSException("New password must be different from old password");
		}

		if (!newPassword.equals(confirmPassword)) {
			throw new PWSException("New password and confirm password does not match");
		}

		user.setPassword(encoder.encode(newPassword));
		user.setResetPasswordOtp(null);
		user.setResetPasswordExpiry(null);
		userRepository.save(user);
	}










	@Override
	public void addRole(Role role) throws PWSException {
		role.setIsActive(true);
		roleRepository.save(role);
	}

	@Override
	public void updateRole(Role role) throws PWSException {
		System.out.println("fetched from DB");
		roleRepository.save(role);
	}


	@Override
	public List<Role> fetchAllRole() throws PWSException {
		System.out.println("fetched from DB");
		return roleRepository.findAll();
	}


	@Override
	public Optional<Role> fetchRoleById(Integer id) throws PWSException {
		System.out.println("fetched from DB");
		return roleRepository.findById(id);
	}




	@Override
	public void deactivateOrActivateRoleById(Integer id, boolean flag) throws PWSException {
		Optional<Role> role = roleRepository.findById(id);
		if (role.isPresent()) {
			role.get().setIsActive(flag);
			roleRepository.save(role.get());
		}
	}

	@Override
	public void addModule(Module module) throws PWSException {
		moduleRepository.save(module);
	}

	@Override
	public void updateModule(Module module) throws PWSException {
		System.out.println("fetched from DB");
		Optional<Module> optionalModule = moduleRepository.findById(module.getId());
		if (optionalModule.isPresent()) {
			moduleRepository.save(module);
		} else
			throw new PWSException("Module Doest Exist");

	}

	@Override
	public List<Module> fetchAllModule() throws PWSException {
		System.out.println("fetched from DB");
		return moduleRepository.findAll();
	}

	@Override
	public Optional<Module> fetchModuleById(Integer id) throws PWSException {
		System.out.println("fetched from DB");
		Optional<Module> optionalModule = moduleRepository.findById(id);
		if (optionalModule.isPresent()) {
			return optionalModule;
		} else
			throw new PWSException("Module Doest Exist");
	}

	@Override
	public void deactivateOrActivateModuleById(Integer id, boolean flag) throws PWSException {
		Optional<Module> optionalModule = moduleRepository.findById(id);
		Module module = optionalModule.get();
		if (optionalModule.isPresent()) {
			module.setIsActive(flag);
			moduleRepository.save(module);
		} else
			throw new PWSException("Module Doest Exist");
	}

	@Override
	public void saveOrUpdateUserXref(UserRoleXrefDTO userRoleXrefDTO) throws PWSException {
		Optional<UserRoleXref> optionalUserRoleXref = userRoleXrefRepository.findById(userRoleXrefDTO.getId());
		UserRoleXref userRoleXref = null;
		if (optionalUserRoleXref.isPresent()) {
			userRoleXref = optionalUserRoleXref.get();
		} else {
			userRoleXref = new UserRoleXref();
		}
		Optional<User> optionalUser = userRepository.findById(userRoleXrefDTO.getUserId());
		if (optionalUser.isPresent()) {
			userRoleXref.setUser(optionalUser.get());
		} else {
			throw new PWSException("User Doest Exist");
		}

		Optional<Role> optionalRole = roleRepository.findById(userRoleXrefDTO.getRoleId());
		if (optionalRole.isPresent()) {
			userRoleXref.setRole(optionalRole.get());
		} else {
			throw new PWSException("Role Doest Exist");
		}
		userRoleXref.setIsActive(userRoleXrefDTO.getIsActive());

		userRoleXrefRepository.save(userRoleXref);

	}

	@Override
	public void deactivateOrActivateAssignedRoleToUser(Integer id, Boolean flag) throws PWSException {
		Optional<UserRoleXref> optionalUserRoleXref = userRoleXrefRepository.findById(id);
		UserRoleXref userRoleXref = optionalUserRoleXref.get();
		if (optionalUserRoleXref.isPresent()) {
			optionalUserRoleXref.get().setIsActive(flag);
			userRoleXrefRepository.save(userRoleXref);
		} else
			throw new PWSException("Record Doest Exist");

	}

	@Override
	public Optional<UserRoleXref> fetchUserById(Integer Id) throws PWSException {
		System.out.println("fetched from DB");
		return userRoleXrefRepository.findById(Id);

	}

	@Override
	public List<User> fetchUserByRole(Integer roleId) throws PWSException {
		System.out.println("fetched from DB");
		return userRoleXrefRepository.fetchAllUsersByRoleId(roleId);
	}

	@Override
	public void addPermission(PermissionDTO permissionDTO) throws PWSException {
		Permission permission = new Permission();

		permission.setIsActive(permissionDTO.getIsActive());
		permission.setIsAdd(permissionDTO.getIsAdd());
		permission.setIsDelete(permissionDTO.getIsDelete());
		permission.setIsUpdate(permissionDTO.getIsUpdate());
		permission.setIsView(permissionDTO.getIsView());
		Optional<Module> module = moduleRepository.findById(permissionDTO.getModule());
		permission.setModule(module.get());
		Optional<Role> role = roleRepository.findById(permissionDTO.getRole());
		permission.setRole(role.get());
		permissionRepository.save(permission);

	}

	@Override
	public void updatePermission(PermissionDTO permissionDTO) throws PWSException {
		System.out.println("fetched from DB");
		Optional<Permission> optionalpermission = permissionRepository.findById(permissionDTO.getId());
		if (optionalpermission.isPresent()) {
			optionalpermission.get().getId();
			optionalpermission.get().setIsActive(permissionDTO.getIsActive());
			optionalpermission.get().setIsAdd(permissionDTO.getIsAdd());
			optionalpermission.get().setIsDelete(permissionDTO.getIsDelete());
			optionalpermission.get().setIsUpdate(permissionDTO.getIsUpdate());
			optionalpermission.get().setIsView(permissionDTO.getIsView());
			Optional<Module> module = moduleRepository.findById(permissionDTO.getModule());
			optionalpermission.get().setModule(module.get());
			Optional<Role> role = roleRepository.findById(permissionDTO.getRole());
			optionalpermission.get().setRole(role.get());
			permissionRepository.save(optionalpermission.get());

		} else {
			throw new PWSException("Record Doest Exist");
		}

	}

	@Override
	public List<Permission> fetchAllPermission() throws PWSException {
		System.out.println("fetched from DB");
		List<Permission> permissionlist = permissionRepository.findAll();
		return permissionlist;
	}

	@Override
	public Optional<Permission> fetchPermissionById(Integer id) throws PWSException {
		System.out.println("fetched from DB");
		Optional<Permission> optionalpermission = permissionRepository.findById(id);
		if (optionalpermission.isPresent()) {
			return optionalpermission;
		} else
			throw new PWSException("Permission Does't Exist");
	}

	@Override
	public void deactivateOrActivatePermissionById(PermissionDTO permissionDTO) throws PWSException {
		Optional<Permission> optionalPermission = permissionRepository.findById(permissionDTO.getId());
		Permission permission = null;
		if (optionalPermission.isPresent()) {
			permission = optionalPermission.get();
			permission.setIsActive(permissionDTO.getIsActive());
			permissionRepository.save(permission);
		} else

			throw new PWSException("Permission Id Doest Exist");

	}

	@Override
	public UserBasicDetailsDTO getUserBasicInfoAfterLoginSuccess(String email) throws PWSException {
		Optional<User> optionalUser = userRepository.findUserByEmail(email);
		if(! optionalUser.isPresent())
			throw new PWSException("User Not Exist with Email : " + email);


		User user = optionalUser.get();
		UserBasicDetailsDTO userBasicDetailsDTO =new UserBasicDetailsDTO();
		userBasicDetailsDTO.setUser(user);

		List<Role> roleList = userRoleXrefRepository.findAllUserRoleByUserId(user.getId());
		userBasicDetailsDTO.setRoleList(roleList);
		List<Permission> permissionList =null;
		if(roleList.size()>0)
			permissionList = permissionRepository.getAllUserPermisonsByRoleId(roleList.get(0).getId());

		userBasicDetailsDTO.setPermissionList(permissionList);
		return userBasicDetailsDTO;
	}

	@Override
	public void addskill(Skill skill) throws PWSException {
		skill.setIsActive(true);
		skillRepository.save(skill);
	}

	@Override
	public void updateskill(Skill skill) throws PWSException {
		System.out.println("fetched from DB");
		Optional<Skill> optionalskill= skillRepository.findById(skill.getId());
		if(optionalskill.isPresent()) {
			skillRepository.save(skill);
		}else
			throw new PWSException("Skill doesn't exist");
	}

	@Override
	public List<Skill> fetchAllSkills() throws PWSException {
		System.out.println("fetched from DB");
		return skillRepository.findAll();

	}

	@Override
	public Optional<Skill> fetchskillById(Integer id) throws PWSException {
		System.out.println("fetched from DB");
		Optional<Skill> optionalskill= skillRepository.findById(id);
		if(optionalskill.isPresent()) {
			return skillRepository.findById(id);
		}else
			throw new PWSException("Skill doesn't exist");
	}

	@Override
	public void deleteskillById(Integer id) throws PWSException {
		skillRepository.deleteById(id);
	}

}
