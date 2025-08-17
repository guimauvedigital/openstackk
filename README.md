# openstackk

A pure Kotlin client for OpenStack, compatible with Kotlin Multiplatform and Coroutines.

> This project is in early development and is not yet ready for production use.

## Motivation

We made `openstackk` to fix the issues we encountered with existing OpenStack Java client:

- It is not multiplatform, so it cannot be used in Kotlin Multiplatform projects.
- It does not provide null annotations, make NullPointerExceptions a common occurrence.
- It is not designed for Kotlin Coroutines, so it does not provide a good experience when used with Kotlin Coroutines.
- It has a weird way of managing authentication, relying on threads to store the authentication state, which is really
  difficult to use in Kotlin Coroutines projects.

So we decided to create a new client from scratch, using Kotlin Multiplatform and Coroutines.

## Installation

Coming soon.

## Usage

Coming soon.
