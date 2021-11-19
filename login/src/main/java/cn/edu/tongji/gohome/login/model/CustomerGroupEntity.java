package cn.edu.tongji.gohome.login.model;

import javax.persistence.*;
import java.util.Objects;

/**
 * CustomerGroupEntity: JPA 自动生成
 *
 * @author 卓正一
 * @since  2021/11/19 6:47 PM
 */
@Entity
@Table(name = "customer_group", schema = "GoHome", catalog = "")
public class CustomerGroupEntity {
    private int customerLevel;
    private String customerLevelName;
    private long customerLevelDegree;

    @Id
    @Column(name = "customer_level")
    public int getCustomerLevel() {
        return customerLevel;
    }

    public void setCustomerLevel(int customerLevel) {
        this.customerLevel = customerLevel;
    }

    @Basic
    @Column(name = "customer_level_name")
    public String getCustomerLevelName() {
        return customerLevelName;
    }

    public void setCustomerLevelName(String customerLevelName) {
        this.customerLevelName = customerLevelName;
    }

    @Basic
    @Column(name = "customer_level_degree")
    public long getCustomerLevelDegree() {
        return customerLevelDegree;
    }

    public void setCustomerLevelDegree(long customerLevelDegree) {
        this.customerLevelDegree = customerLevelDegree;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerGroupEntity that = (CustomerGroupEntity) o;
        return customerLevel == that.customerLevel && customerLevelDegree == that.customerLevelDegree && Objects.equals(customerLevelName, that.customerLevelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerLevel, customerLevelName, customerLevelDegree);
    }
}
