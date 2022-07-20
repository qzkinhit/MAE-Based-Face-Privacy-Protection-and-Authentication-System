# models.py

from sqlalchemy import Column, String, Integer, BINARY

from database import Base, engine




class User(Base):
    __tablename__ = 'user'  # 数据库表名

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(255), nullable=False)
    face = Column(String, nullable=False)
    appendant = Column(Integer, nullable=False)
    meanVar = Column(BINARY, nullable=False)


if __name__ == '__main__':
    Base.metadata.create_all(engine)
