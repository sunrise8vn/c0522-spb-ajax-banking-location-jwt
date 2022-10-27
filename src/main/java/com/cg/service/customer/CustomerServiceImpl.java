package com.cg.service.customer;


import com.cg.exception.DataInputException;
import com.cg.model.*;
import com.cg.model.dto.CustomerDTO;
import com.cg.model.dto.ICustomerDTO;
import com.cg.model.dto.RecipientDTO;
import com.cg.model.enums.FileType;
import com.cg.repository.*;
import com.cg.service.upload.UploadService;
import com.cg.util.UploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@Transactional
public class CustomerServiceImpl implements ICustomerService {


    @Autowired
    private LocationRegionRepository locationRegionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private WithdrawRepository withdrawRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UploadUtil uploadUtil;

    @Override
    public List<Customer> findAllByDeletedIsFalse() {
        return customerRepository.findAllByDeletedIsFalse();
    }

    @Override
    public List<CustomerDTO> getAllCustomerDTOByDeletedIsFalse() {
        return customerRepository.getAllCustomerDTOByDeletedIsFalse();
    }

    @Override
    public List<ICustomerDTO> getAllICustomerDTOByDeletedIsFalse() {
        return customerRepository.getAllICustomerDTOByDeletedIsFalse();
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public List<Customer> findAllByIdNot(long id) {
        return customerRepository.findAllByIdNot(id);
    }

    @Override
    public Customer getById(Long id) {
        return null;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Boolean existsByIdEquals(long id) {
        return customerRepository.existsById(id);
    }

    @Override
    public List<Customer> getAllByIdNot(long senderId) {
        return customerRepository.getAllByIdNot(senderId);
    }

    @Override
    public List<RecipientDTO> getAllRecipientDTO(long senderId) {
        return customerRepository.getAllRecipientDTO(senderId);
    }

    @Override
    public Customer deposit(Customer customer, Deposit deposit) {
        BigDecimal currentBalance = customer.getBalance();
        BigDecimal transactionAmount = deposit.getTransactionAmount();
//        BigDecimal newBalance = currentBalance.add(transactionAmount);

        try {
//            customer.setBalance(newBalance);
//            Customer newCustomer = customerRepository.save(customer);

            customerRepository.incrementBalance(transactionAmount, customer.getId());

            deposit.setId(0L);
            deposit.setCustomer(customer);
            depositRepository.save(deposit);

            Optional<Customer> newCustomer = customerRepository.findById(customer.getId());

            return newCustomer.get();
        } catch (Exception e) {
            e.printStackTrace();
            customer.setBalance(currentBalance);
            return customer;
        }
    }

    @Override
    public Customer withdraw(Customer customer, Withdraw withdraw) {
        BigDecimal currentBalance = customer.getBalance();
        BigDecimal transactionAmount = withdraw.getTransactionAmount();

        try {
            customerRepository.reduceBalance(transactionAmount, customer.getId());

            withdraw.setId(0L);
            withdraw.setCustomer(customer);
            withdrawRepository.save(withdraw);

            Optional<Customer> newCustomer = customerRepository.findById(customer.getId());

            return newCustomer.get();
        } catch (Exception e) {
            e.printStackTrace();
            customer.setBalance(currentBalance);
            return customer;
        }
    }

    @Override
    public Customer transfer(Transfer transfer) {
//        Customer sender = transfer.getSender();
//        sender.setBalance(sender.getBalance().subtract(transfer.getTransactionAmount()));
//        Customer newSender = customerRepository.save(sender);

        customerRepository.reduceBalance(transfer.getTransactionAmount(), transfer.getSender().getId());

//        Customer recipient = transfer.getRecipient();
//        recipient.setBalance(recipient.getBalance().add(transfer.getTransferAmount()));
//        customerRepository.save(recipient);

        customerRepository.incrementBalance(transfer.getTransferAmount(), transfer.getRecipient().getId());

        transferRepository.save(transfer);

        Optional<Customer> newSender = customerRepository.findById(transfer.getSender().getId());

        return newSender.get();
    }

    @Override
    public Customer save(Customer customer) {
        return null;
    }

    @Override
    public Customer saveWithAvatar(Customer customer, MultipartFile avatarFile) {

        LocationRegion locationRegion = customer.getLocationRegion();
        locationRegion.setId(0L);
        LocationRegion newLocationRegion = locationRegionRepository.save(locationRegion);

        customer.setLocationRegion(newLocationRegion);

        Customer newCustomer = customerRepository.save(customer);

        Avatar avatar = new Avatar();

        String fileType = avatarFile.getContentType();

        assert fileType != null;

        fileType = fileType.substring(0, 5);

        avatar.setFileType(fileType);
        avatar.setCustomer(newCustomer);

        Avatar newAvatar = avatarRepository.save(avatar);

        if (fileType.equals(FileType.IMAGE.getValue())) {
            uploadAndSaveProductImage(avatarFile, newCustomer, newAvatar);
        }

        return newCustomer;
    }

    private void uploadAndSaveProductImage(MultipartFile avatarFile, Customer customer, Avatar avatar) {
        try {
            Map uploadResult = uploadService.uploadImage(avatarFile, uploadUtil.buildImageUploadParams(avatar));
            String fileUrl = (String) uploadResult.get("secure_url");
            String fileFormat = (String) uploadResult.get("format");

            avatar.setFileName(avatar.getId() + "." + fileFormat);
            avatar.setFileUrl(fileUrl);
            avatar.setFileFolder(UploadUtil.IMAGE_UPLOAD_FOLDER);
            avatar.setCloudId(avatar.getFileFolder() + "/" + avatar.getId());
            avatar.setCustomer(customer);
            avatarRepository.save(avatar);

        } catch (IOException e) {
            e.printStackTrace();
            throw new DataInputException("Upload hình ảnh thất bại");
        }
    }

    @Override
    public void remove(Long id) {

    }

    @Override
    public void softDelete(long customerId) {
        customerRepository.softDelete(customerId);
    }
}
