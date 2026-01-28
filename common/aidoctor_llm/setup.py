from setuptools import setup, find_packages

setup(
    name="aidoctor-llm-common",
    version="0.1.0",
    description="AI医生系统LLM公共库",
    author="AI医生开发团队",
    packages=find_packages(),
    install_requires=[
        "langchain>=0.1.0",
        "langchain-openai>=0.0.2",
        "langchain-community>=0.0.10",
        "langchain-core>=0.1.0",
        "openai==1.10.0",
        "jinja2==3.1.2",
        "pyyaml==6.0.1",
        "httpx==0.25.1",
        "pydantic>=2.7.0",
    ],
    python_requires=">=3.10",
)

