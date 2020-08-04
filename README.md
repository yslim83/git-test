# 서비스 시나리오

기능적 요구사항
1. 고객이 마스크를 주문한다
1. 주문이 되면 주문내역이 배송팀에 전달된다
1. 주문이 되면 주문내역이 재고팀에 전달된다
 (상품팀이 상품을 등록한다)
1. 고객이 주문을 취소할 수 있다
1. 주문이 취소되면 배송이 취소된다
1. 주문이 취소되면 재고가 변경된다
 (상품팀이 상품을 변경할 수 있다)
 (상품정보가 변경되면 재고정보가 변경된다)

비기능적 요구사항
1. 트랜잭션
    배송팀에 할당되지 않은 주문건은 주문이 성립되지 않아야 한다  Sync 호출 
1. 장애격리
    (.....)
1. 성능
    1. 고객이 MyPage에서 주문정보를 확인할 수 있어야 한다  CQRS
    (1. 배달상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven)




# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://msaez.io/#/storming/myQFPsZHyPTzv4dF1Rp3utSmqZ22/share/373ad0f49c8377342299860271bed963/-MDnu4FWQAjhi_RHUFmd


### 이벤트 도출
![image](https://github.com/yslim83/git-test/blob/master/report_images_1/eventstorming_team_cmd_evt.png)

### 어그리게잇과 바운디드 컨텍스트로 묶기
![image](https://github.com/yslim83/git-test/blob/master/report_images_1/eventstorming_team_aggr_bc.png)

    - 주문, 배송, 재고와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위끼리 묶어줌

    - 도메인 서열 분리 
        - Core Domain:  order(주문), delivery(배송)
        - Supporting Domain:  inventory(재고)
        - General Domain:   My Page

### 폴리시 부착 

![image](https://github.com/yslim83/git-test/blob/master/report_images_1/eventstorming_team_policy.png)

    - 재고의 event를 삭제

### 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://github.com/yslim83/git-test/blob/master/report_images_1/eventstorming_team.png)


### 기능적/비기능적 요구사항을 커버하는지 검증

![image](https://github.com/yslim83/git-test/blob/master/report_images_1/eventstorming_team_1_func.png)

    - 고객이 마스크를 주문한다
    - 배송팀에 주문내역이 전달된다
    - 재고수량에서 주문수량만큼 차감된다

![image](https://github.com/yslim83/git-test/blob/master/report_images_1/eventstorming_team_2_func.png)
    - 고객이 주문을 취소할 수 있다
    - 주문이 취소되면 배송 상태값이 변경된다
    - 주문이 취소되면 취소된 수량만큼 재고 수량이 증가한다 


### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/487999/79684184-5c9a9400-826a-11ea-8d87-2ed1e44f4562.png)

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
        - 고객 주문시 결제처리:  결제가 완료되지 않은 주문은 절대 받지 않는다는 경영자의 오랜 신념(?) 에 따라, ACID 트랜잭션 적용. 주문와료시 결제처리에 대해서는 Request-Response 방식 처리
        - 결제 완료시 점주연결 및 배송처리:  App(front) 에서 Store 마이크로서비스로 주문요청이 전달되는 과정에 있어서 Store 마이크로 서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
        - 나머지 모든 inter-microservice 트랜잭션: 주문상태, 배달상태 등 모든 이벤트에 대해 카톡을 처리하는 등, 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.




## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/487999/79684772-eba9ab00-826e-11ea-9405-17e2bf39ec76.png)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd app
mvn spring-boot:run

cd pay
mvn spring-boot:run 

cd store
mvn spring-boot:run  

cd customer
python policy-handler.py 
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 pay 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 하지만, 일부 구현에 있어서 영문이 아닌 경우는 실행이 불가능한 경우가 있기 때문에 계속 사용할 방법은 아닌것 같다. (Maven pom.xml, Kafka의 topic id, FeignClient 의 서비스 id 등은 한글로 식별자를 사용하는 경우 오류가 발생하는 것을 확인하였다)

```
package fooddelivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="결제이력_table")
public class 결제이력 {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String orderId;
    private Double 금액;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public Double get금액() {
        return 금액;
    }

    public void set금액(Double 금액) {
        this.금액 = 금액;
    }

}
