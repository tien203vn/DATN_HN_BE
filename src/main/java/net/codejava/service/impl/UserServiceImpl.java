package net.codejava.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.codejava.constant.DefaultAvatar;
import net.codejava.domain.dto.auth.LoginResponseDTO;
import net.codejava.domain.dto.meta.MetaRequestDTO;
import net.codejava.domain.dto.meta.MetaResponseDTO;
import net.codejava.domain.dto.user.AddUserRequestDTO;
import net.codejava.domain.dto.user.UpdUserRequestDTO;
import net.codejava.domain.dto.user.UserBookingCountDTO;
import net.codejava.domain.dto.user.UserDetailResponseDTO;
import net.codejava.domain.entity.Image;
import net.codejava.domain.entity.User;
import net.codejava.domain.mapper.UserMapper;
import net.codejava.exceptions.AppException;
import net.codejava.repository.BookingRepository;
import net.codejava.repository.ImageRepository;
import net.codejava.repository.UserRepository;
import net.codejava.responses.MetaResponse;
import net.codejava.responses.Response;
import net.codejava.service.CloudinaryService;
import net.codejava.service.UserService;
import net.codejava.utility.JwtTokenUtil;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserMapper userMapper;
    private final ImageRepository imageRepo;
    private final UserRepository userRepo;
    private final JwtTokenUtil jwtTokenUtil;
    private final BookingRepository bookingRepo;
    private final CloudinaryService cloudinaryService;

    @Value("${cloudinary.folder.avatar}")
    private String folderAvatar;

    @Override
    public Response<UserDetailResponseDTO> getDetailUser(Integer id) {
        Optional<User> findUser = userRepo.findById(id);
        if (findUser.isEmpty()) throw new AppException("This user is not existed");
        return Response.successfulResponse(
                "Get detail user successful", userMapper.toUserDetailResponseDTO(findUser.get()));
    }

    @Override
    public Response<LoginResponseDTO> addUser(AddUserRequestDTO requestDTO) throws IOException {
        Optional<User> findUser = userRepo.findByEmail(requestDTO.email());
        if (!findUser.isEmpty()) throw new AppException("Email already existed. Please try another email.");

        User newUser = userMapper.addUserRequestDTOtoUserEntity(requestDTO);
        // Set password
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        newUser.setPassword(passwordEncoder.encode(requestDTO.password()));
        // Set Image
        Image avatarUpload;
        // if (requestDTO.avatar() == null || requestDTO.avatar().isEmpty()) {
        // Image defaultImage = imageRepo.findById(1).get();
        avatarUpload = Image.builder()
                .name(DefaultAvatar.avatarName)
                .imageUrl(DefaultAvatar.avatarUrl)
                .imagePublicId(DefaultAvatar.publicIdAvatar)
                .whenCreated(new Date())
                .build();
        //        } else {
        //            Map uploadImage = cloudinaryService.uploadFile(requestDTO.avatar(), folderAvatar);
        //            avatarUpload = Image.builder()
        //                    .name((String) uploadImage.get("original_filename"))
        //                    .imageUrl((String) uploadImage.get("url"))
        //                    .imagePublicId((String) uploadImage.get("public_id"))
        //                    .whenCreated(new Date())
        //                    .build();
        //        }
        newUser.setAvatar(avatarUpload);

        try {
            User saveUser = userRepo.save(newUser);
            return Response.successfulResponse(
                    "Register successful",
                    LoginResponseDTO.builder()
                            .authenticated(true)
                            .token(jwtTokenUtil.generateToken(saveUser))
                            .infor(userMapper.toUserDetailResponseDTO(saveUser))
                            .build());
        } catch (Exception e) {
            throw new AppException("Register fail");
        }
    }

    @Override
    public Response<UserDetailResponseDTO> updateUser(Integer id, UpdUserRequestDTO requestDTO) {
        Optional<User> oldUser = userRepo.findById(id);
        if (oldUser.isEmpty()) throw new AppException("This user is not existed");

        User newUser = userMapper.updUserToUserEntity(oldUser.get(), requestDTO);
        try {
            User saveUser = userRepo.save(newUser);
            return Response.successfulResponse("Update User Successfull", userMapper.toUserDetailResponseDTO(saveUser));
        } catch (Exception e) {
            throw new AppException("Update User fail");
        }
    }

    @Override
    public MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>> getListUserBooking(
            MetaRequestDTO requestDTO, Integer ownerId) {
        Pageable pageable = PageRequest.of(requestDTO.currentPage() - 1, 100);
        List<Object[]> results =
                bookingRepo.getListCustomerWithBookingCountByOwnerId(ownerId, requestDTO.keyword(), pageable);

        if (results.isEmpty()) throw new AppException("Danh sách khách hàng trống");

        List<UserBookingCountDTO> li = results.stream()
                .map(obj -> UserBookingCountDTO.builder()
                        .user(userMapper.toUserDetailResponseDTO((User) obj[0]))
                        .bookingCount((Long) obj[1])
                        .build())
                .toList();

        MetaResponseDTO meta = MetaResponseDTO.builder()
                .totalItems(li.size())
                .totalPages((int) Math.ceil((double) li.size() / 10))
                .currentPage(requestDTO.currentPage()-1)
                .pageSize(10)
                .sorting(net.codejava.domain.dto.meta.SortingDTO.builder()
                        .sortField(requestDTO.sortField())
                        .sortDir(requestDTO.sortDir())
                        .build())
                .build();

        return MetaResponse.successfulResponse("Lấy danh sách khách hàng thành công", meta, li);
    }

    @Override
    public MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>> getListUser(
            MetaRequestDTO requestDTO) {
        Pageable pageable = PageRequest.of(requestDTO.currentPage() -1, 100);
        List<Object[]> results =
                bookingRepo.getListCustomer( requestDTO.keyword(), pageable);

        if (results.isEmpty()) throw new AppException("Danh sách khách hàng trống");

        List<UserBookingCountDTO> li = results.stream()
                .map(obj -> UserBookingCountDTO.builder()
                        .user(userMapper.toUserDetailResponseDTO((User) obj[0]))
                        .bookingCount((Long) obj[1])
                        .build())
                .toList();

        MetaResponseDTO meta = MetaResponseDTO.builder()
                .totalItems(li.size())
                .totalPages((int) Math.ceil((double) li.size() / 10))
                .currentPage(requestDTO.currentPage()-1)
                .pageSize(10)
                .sorting(net.codejava.domain.dto.meta.SortingDTO.builder()
                        .sortField(requestDTO.sortField())
                        .sortDir(requestDTO.sortDir())
                        .build())
                .build();

        return MetaResponse.successfulResponse("Lấy danh sách khách hàng thành công", meta, li);
    }

    @Override
    public Response<Map<String, String>> getMyWallet(Integer userId) {
        Optional<User> findUser = userRepo.findById(userId);
        if (findUser.isEmpty()) throw new AppException("This user is not existed");

        Map<String, String> res =
                Map.ofEntries(Map.entry("wallet", findUser.get().getWallet().toString()));
        return Response.successfulResponse("Get your wallet successful", res);
    }

    @Override
    public MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>> getListUserWithPickupOrConfirmOrders(
            MetaRequestDTO requestDTO, Integer ownerId) {
        Pageable pageable = PageRequest.of(requestDTO.currentPage(), requestDTO.pageSize());
        List<Object[]> results = bookingRepo.getListCustomerWithPickupOrConfirmOrders(ownerId, pageable);

        if (results.isEmpty()) throw new AppException("Danh sách khách hàng trống");

        List<UserBookingCountDTO> li = results.stream()
                .map(obj -> UserBookingCountDTO.builder()
                        .user(userMapper.toUserDetailResponseDTO((User) obj[0]))
                        .bookingCount((Long) obj[1])
                        .build())
                .toList();

        MetaResponseDTO meta = MetaResponseDTO.builder()
                .totalItems(li.size())
                .totalPages(1)
                .currentPage(requestDTO.currentPage())
                .pageSize(requestDTO.pageSize())
                .sorting(net.codejava.domain.dto.meta.SortingDTO.builder()
                        .sortField(requestDTO.sortField())
                        .sortDir(requestDTO.sortDir())
                        .build())
                .build();

        return MetaResponse.successfulResponse("Lấy danh sách khách hàng thành công", meta, li);
    }

    @Override
    public Response<String> toggleUserStatus(Integer userId) {
        Optional<User> userOptional = userRepo.findById(userId);
        if (userOptional.isEmpty()) {
            throw new AppException("Không tìm thấy người dùng với ID: " + userId);
        }

        User user = userOptional.get();
        user.setActive(!user.isActive());
        user.setUpdatedAt(new Date());
        userRepo.save(user);

        String status = user.isActive() ? "kích hoạt" : "vô hiệu hóa";
        String message = "Đã " + status + " tài khoản người dùng thành công";

        return Response.successfulResponse(message, message);
    }

    @Override
    public Response<String> deleteUser(Integer userId) {
        Optional<User> userOptional = userRepo.findById(userId);
        if (userOptional.isEmpty()) {
            throw new AppException("Không tìm thấy người dùng với ID: " + userId);
        }

        User user = userOptional.get();

        // Kiểm tra xem user có đang có booking đang hoạt động không
        boolean hasActiveBookings = bookingRepo.existsByUserAndStatusIn(
                user,
                List.of("PENDING_DEPOSIT", "PICK_UP")
        );

        if (hasActiveBookings) {
            throw new AppException("Không thể xóa người dùng vì đang có booking đang hoạt động");
        }

        // Xóa avatar từ cloudinary nếu có
//        if (user.getAvatar() != null && !user.getAvatar().getUrl().equals(DefaultAvatar.AVATAR_URL)) {
//            try {
//                cloudinaryService.deleteImage(user.getAvatar().getPublicId());
//            } catch (Exception e) {
//                // Log lỗi nhưng vẫn tiếp tục xóa user
//                log.error("Lỗi khi xóa avatar từ cloudinary: {}", e.getMessage());
//            }
//        }

        userRepo.delete(user);
        return Response.successfulResponse("Xóa tài khoản người dùng thành công", "Xóa tài khoản người dùng thành công");
    }
}
