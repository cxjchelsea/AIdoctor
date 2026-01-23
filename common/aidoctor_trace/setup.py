from setuptools import setup, find_packages

setup(
    name="aidoctor-trace",
    version="1.0.0",
    description="AI医生执行追踪公共库",
    packages=find_packages(),
    install_requires=[
        "httpx>=0.24.0",
    ],
    python_requires=">=3.8",
)

