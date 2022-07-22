#
# Copyright (c) 2022 Airbyte, Inc., all rights reserved.
#


from setuptools import find_packages, setup

MAIN_REQUIREMENTS = [
    "airbyte-cdk~=0.1.56",
    "pytest~=7.1.2",
]

TEST_REQUIREMENTS = [
    "pytest~=7.1.2",
    "pytest-mock",
    "source-acceptance-test",
]

setup(
    name="source_yandex_metrica",
    description="Source implementation for Yandex Metrica.",
    author="Airbyte",
    author_email="contact@airbyte.io",
    packages=find_packages(),
    install_requires=MAIN_REQUIREMENTS,
    package_data={"": ["*.json", "*.yaml", "schemas/*.json", "schemas/shared/*.json"]},
    extras_require={
        "tests": TEST_REQUIREMENTS,
    },
)
