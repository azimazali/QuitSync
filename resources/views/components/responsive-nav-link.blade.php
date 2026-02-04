@props(['active'])

@php
    $classes = ($active ?? false)
        ? 'block w-full ps-3 pe-4 py-2 border-l-4 border-emerald-400 text-start text-base font-medium text-white bg-emerald-700 focus:outline-none focus:text-white focus:bg-emerald-700 focus:border-emerald-400 transition duration-150 ease-in-out'
        : 'block w-full ps-3 pe-4 py-2 border-l-4 border-transparent text-start text-base font-medium text-emerald-100 hover:text-white hover:bg-emerald-500 hover:border-emerald-300 focus:outline-none focus:text-white focus:bg-emerald-500 focus:border-emerald-300 transition duration-150 ease-in-out';

@endphp

<a {{ $attributes->merge(['class' => $classes]) }}>
    {{ $slot }}
</a>