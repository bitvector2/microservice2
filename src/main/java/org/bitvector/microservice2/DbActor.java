package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.actor.Status.Success;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

public class DbActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private EntityManagerFactory emf;

    public DbActor() {
        receive(ReceiveBuilder
                        .match(Start.class, this::start)
                        .match(Stop.class, this::stop)
                        .match(GetAllProducts.class, this::getAllProducts)
                        .match(GetAProduct.class, this::getProductById)
                        .match(AddProduct.class, this::addProduct)
                        .match(UpdateProduct.class, this::updateProduct)
                        .match(DeleteProduct.class, this::deleteProduct)
                        .matchAny(obj -> log.error("DbActor received unknown message " + obj.toString()))
                        .build()
        );
    }

    private void start(Start msg) {
        try {
            emf = Persistence.createEntityManagerFactory("microservice");
        } catch (PersistenceException e) {
            log.error("Failed to create DB actor: " + e.getMessage());
            e.printStackTrace();
            getContext().stop(self());
        }
    }

    private void stop(Stop msg) {
        emf.close();
    }

    private void getAllProducts(GetAllProducts msg) {
        EntityManager em = emf.createEntityManager();
        TypedQuery<ProductEntity> query = em.createQuery("SELECT p FROM ProductEntity p", ProductEntity.class);
        query.setHint("org.hibernate.cacheable", true);
        List<ProductEntity> productEntities = query.getResultList();
        sender().tell(new AllProducts(productEntities), self());
        em.close();
    }

    private void getProductById(GetAProduct msg) {
        EntityManager em = emf.createEntityManager();
        ProductEntity productEntity = em.find(ProductEntity.class, msg.getId());
        sender().tell(new AProduct(productEntity), self());
        em.close();
    }

    private void addProduct(AddProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(msg.getProductEntity());
        tx.commit();
        sender().tell(new Success(true), self());
        em.close();
    }

    private void updateProduct(UpdateProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        ProductEntity productEntity = em.find(ProductEntity.class, msg.getProductEntity().getId());
        if (productEntity != null) {
            tx.begin();
            productEntity.setName(msg.getProductEntity().getName());
            tx.commit();
            sender().tell(new Success(true), self());
        } else {
            sender().tell(new Success(false), self());
        }
        em.close();
    }

    private void deleteProduct(DeleteProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        ProductEntity productEntity = em.find(ProductEntity.class, msg.getProductEntity().getId());
        if (productEntity != null) {
            tx.begin();
            em.remove(productEntity);
            tx.commit();
            sender().tell(new Success(true), self());
        } else {
            sender().tell(new Success(false), self());
        }
        em.close();
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }

    public static class GetAllProducts implements Serializable {
    }

    public static class AllProducts implements Serializable {
        private List<ProductEntity> productEntities;
        public AllProducts(List<ProductEntity> productEntities) {
            this.productEntities = productEntities;
        }
        public List<ProductEntity> getProductEntities() {
            return productEntities;
        }
    }

    public static class GetAProduct implements Serializable {
        private Integer id;

        public GetAProduct(Integer id) {
            this.id = id;
        }
        public Integer getId() {
            return id;
        }
    }

    public static class AProduct implements Serializable {
        private ProductEntity productEntity;
        public AProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }
        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }

    public static class AddProduct implements Serializable {
        private ProductEntity productEntity;
        public AddProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }
        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }

    public static class UpdateProduct implements Serializable {
        private ProductEntity productEntity;
        public UpdateProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }
        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }

    public static class DeleteProduct implements Serializable {
        private ProductEntity productEntity;
        public DeleteProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }
        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }
}
