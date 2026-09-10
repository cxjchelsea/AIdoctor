from setuptools import setup

setup(
    name="aidoctor-llm-common",
    version="0.1.0",
    description="AI医生系统LLM公共库（LEGACY DISABLED compatibility facade）",
    author="AI医生开发团队",
    # 包根即当前目录（含 __init__.py）
    packages=["aidoctor_llm"],
    package_dir={"aidoctor_llm": "."},
    install_requires=[
        "pydantic>=2.5.0",
    ],
    python_requires=">=3.10",
)
